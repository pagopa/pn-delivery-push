const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");

const { DynamoDBDocument } = require("@aws-sdk/lib-dynamodb");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const { NodeHttpHandler } = require("@aws-sdk/node-http-handler");

const ddbClient = new DynamoDBClient();

const ddbDocClient = DynamoDBDocument.from(ddbClient, {
  marshallOptions: {
    removeUndefinedValues: true,
  },
});

const DEFAULT_SOCKET_TIMEOUT = 1000;
const DEFAULT_REQUEST_TIMEOUT = 1000;
const DEFAULT_CONNECTION_TIMEOUT = 1000;

const { v4: uuidv4 } = require("uuid");

let sqsParams = { region: process.env.REGION };
if (process.env.ENDPOINT) sqsParams.endpoint = process.env.ENDPOINT;
//const sqs = new SQSClient(sqsParams);

const sqs = new SQSClient({
  requestHandler: new NodeHttpHandler({
    connectionTimeout: DEFAULT_CONNECTION_TIMEOUT,
    requestTimeout: DEFAULT_REQUEST_TIMEOUT,
    socketTimeout: DEFAULT_SOCKET_TIMEOUT,
  }),
});


const QUEUE_URL = process.env.QUEUE_URL;

const TOLLERANCE_IN_MILLIS = 3000;

const isTimeToLeave = (context) =>
  context.getRemainingTimeInMillis() < TOLLERANCE_IN_MILLIS;

const handleEvent = async (event, context) => {
  console.log(JSON.stringify(event, null, 2));
  console.log(QUEUE_URL);

  let futureActions = [];
  let immediateActions = [];

  for (var i = 0; i < event.Records.length; i++) {
    let record = event.Records[i];
    let sequenceNumber = record.kinesis.sequenceNumber;
    let decodedRecord = decodeBase64(record.kinesis.data);

    if (isRecordToSend(decodedRecord)) {
      const action = mapMessageFromKinesisToAction(
        decodedRecord,
        sequenceNumber
      );
      
      
      if (isFutureAction(action.notBefore)) {
        console.log("Is future action ", action.actionId);
        futureActions.push(action);
      } else {
        console.log("Is immediate action ", action.actionId);
        immediateActions.push(action);
      }
    }
  }

  let notSendedImmediateActions = [];
  if (immediateActions.length > 0)
    notSendedImmediateActions = await writeMessagesToQueue(
      immediateActions,
      context
    );
  else console.log("No ImmediateAction to send");

  let notSendedFutureActions = [];
  if (futureActions.length > 0)
    notSendedFutureActions = await writeMessagesToDynamo(
      futureActions,
      context
    );
  else console.log("No futureActions to write");

  let notSendedActions = notSendedImmediateActions.concat(
    notSendedFutureActions
  );
  console.log("notSendedActions ", notSendedActions);

  const result = {
    batchItemFailures: [],
  };

  if (notSendedActions.length !== 0) {
    notSendedActions.forEach((element) =>
      result.batchItemFailures.push({ itemIdentifier: element.kinesisSeqNo })
    );
  }

  console.log("result returned to kinesis is ", result);
  return result;
};

async function writeMessagesToDynamo(futureActions, context) {
  while (futureActions.length > 0 && !isTimeToLeave(context)) {
    let splicedFutureActionsArray = futureActions.splice(0, 1); // prendo i primi 10 e rimuovendoli dall'array originale

    var actionItemMappedDynamoArray = [];
    splicedFutureActionsArray.forEach(function (action) {
      var date = new Date();
      let isoDateNow = date.toISOString();
  
      let futureAction = {
        timeSlot: action.timeslot,
        actionId: action.actionId,
        notBefore: action.notBefore,
        recipientIndex: action.recipientIndex,
        type: action.type,
        timelineId: action.timelineId,
        iun: action.iun,
        details: getActionDetails(action.details),
        insertActionTimestamp: action.insertActionTimestamp,
        insertFutureActionTimestamp: isoDateNow
      };
      console.log("futureAction is ", futureAction);

      var actionItemDynamo = {
        PutRequest: {
          Item: futureAction,
        },
      };

      actionItemMappedDynamoArray.push(actionItemDynamo);
    });

    var params = {
      RequestItems: {
        PocFutureAction: actionItemMappedDynamoArray,
      },
    };

    try {
      console.log("start to batchWrite itemes  ", actionItemMappedDynamoArray);

      let response = await ddbDocClient.batchWrite(params);

      console.log("response received from dynamo is ", response);
      console.log("response.UnprocessedItems ", response.UnprocessedItems);

      // some items are written (but maybe not all of them)
      if (
        response.UnprocessedItems &&
        response.UnprocessedItems.PocFutureAction
      ) {
        console.log(
          "There are unprocessed items ",
          response.UnprocessedItems.PocFutureAction
        );

        splicedFutureActionsArray.forEach(function (splicedFutureAction) {
          if (
            response.UnprocessedItems.PocFutureAction.filter(
              ((unprocessedFutureAction) =>
                unprocessedFutureAction.actionId ==
                splicedFutureAction.actionId).length !== 0
            )
          ) {
            futureActions.push(splicedFutureAction); //Se fallisce nella put, l'action viene reinserita tra quelle da inviare
          }
        });

        return futureActions;
      }
    } catch (exceptions) {
      console.error("Dynamo cannot write items. Exception is", exceptions);
      futureActions = futureActions.concat(splicedFutureActionsArray);
      console.log(
        "splicedFutureActionsArray length ",
        splicedFutureActionsArray.length
      );
      console.log(
        "splicedFutureActionsArray ",
        JSON.stringify(splicedFutureActionsArray)
      );
      console.log("futureActions length", futureActions.length);
      console.log("futureActions ", JSON.stringify(futureActions));
      return futureActions;
    }
  }

  console.log(
    "writeMessagesToDynamo completed. futureActions length is",
    futureActions
  );
  return futureActions;
}

function getActionDetails(actionDetails) {
  if (actionDetails) {
    return {
      quickAccessLinkToken: actionDetails.quickAccessLinkToken,
      key: actionDetails.key,
      documentCreationType: actionDetails.documentCreationType,
      timelineId: actionDetails.timelineId,
      retryAttempt: actionDetails.retryAttempt,
      startWorkflowTime: actionDetails.startWorkflowTime,
      errors: actionDetails.errors,
      isFirstSendRetry: actionDetails.isFirstSendRetry,
      alreadyPresentRelatedFeedbackTimelineId:
        actionDetails.alreadyPresentRelatedFeedbackTimelineId,
      lastAttemptAddressInfo: actionDetails.lastAttemptAddressInfo,
    };
  }
  return actionDetails;
}

function mapActionToQueueMessage(action) {
  let uuid = uuidv4();
  let copiedAction = Object.assign({}, action);
  delete copiedAction.kinesisSeqNo;

  const message = {
    Id: uuid,
    DelaySeconds: 0,
    MessageAttributes: {
      createdAt: {
        DataType: "String",
        StringValue: new Date().toISOString(),
      },
      eventId: {
        DataType: "String",
        StringValue: uuid,
      },
      eventType: {
        DataType: "String",
        StringValue: "ACTION_GENERIC",
      },
      iun: {
        DataType: "String",
        StringValue: action.iun,
      },
      publisher: {
        DataType: "String",
        StringValue: "deliveryPush",
      },
    },
    MessageBody: JSON.stringify(copiedAction),
  };
  return message;
}

function isFutureAction(notBefore) {
  var date = new Date();
  let isoDateNow = date.toISOString();
  console.log("notBefore", notBefore, "and isoDateNow ", isoDateNow);
  return isoDateNow < notBefore;
}

function decodeBase64(encodedRecord) {
  var decodedString = Buffer.from(encodedRecord, "base64").toString();
  let decodedJson = JSON.parse(decodedString);
  console.log("decodedJson", decodedJson);
  return decodedJson;
}

function isRecordToSend(record) {
  console.log("eventName ", record.eventName);
  if (record.eventName != "INSERT") return false;

  // il record è buono e va processato e inviato
  return true;
}

function mapMessageFromKinesisToAction(record, sequenceNumber) {
  console.log("il record e", record);
  let action = record.dynamodb.NewImage;
  console.log("action", action);
  const regularAction = unmarshall(action);
  regularAction.kinesisSeqNo = sequenceNumber;
  console.log("regularAction", regularAction);
  return regularAction;
}

async function writeMessagesToQueue(immediateActions, context) {
  while (immediateActions.length > 0 && !isTimeToLeave(context)) {
    console.log(
      "Proceeding to send " +
        immediateActions.length +
        " messages to " +
        QUEUE_URL
    );

    let splicedActionsArray = immediateActions.splice(0, 1); // prendo i primi 10 e rimuovendoli dall'array originale

    let actionsToSendMapped = [];
    splicedActionsArray.forEach(function (action) {
      let messageToSend = mapActionToQueueMessage(action);
      actionsToSendMapped.push(messageToSend);
    });

    const input = {
      Entries: actionsToSendMapped,
      QueueUrl: QUEUE_URL,
      //requestHandler: defaultRequestHandler,
    };

    console.log("Sending batch message: %j", input);

    const command = new SendMessageBatchCommand(input);
    try {
      const response = await sqs.send(command);
      console.log("Sent message response: %j", response);

      if (response.Failed && response.Failed.length > 0) {
        splicedActionsArray.forEach((element) => {
          if (
            response.Failed.filter((currFailed) => currFailed.Id == element.Id)
              .length !== 0
          )
            immediateActions.push(element); //Se fallisce nella put, l'action viene reinserita tra quelle da inviare
        });

        return immediateActions;
      }
    }
    catch (exceptions) {
      console.log("Error in send sqs message ", exceptions);
      console.log("Stringfy exception ", JSON.stringify(exceptions));
  
      if(exceptions.name && (
          exceptions.name === 'TimeoutError' ||
          exceptions.name === 'RequestTimeout' ||
          exceptions.name === 'RequestTimeoutException')
        ){
        //With timeout error, the record is assumed to be sent by SQS anyway. Need to check with log
        console.error('[SQS_TIMEOUT_ERROR] Timeout record in send sqs, need to check the records ', JSON.stringify(actionsToSendMapped));
      }else{
        console.log('Generic exception in SQS send message, need to reschedule');
        immediateActions = immediateActions.concat(splicedActionsArray);
      }

    }
  }

  return immediateActions;
}

module.exports = { handleEvent };
