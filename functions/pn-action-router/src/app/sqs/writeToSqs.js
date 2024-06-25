const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");
const { NodeHttpHandler } = require("@aws-sdk/node-http-handler");
const { v4: uuidv4 } = require("uuid");
const config = require("config");
const { isTimeToLeave } = require("../utils/utils.js");

const MAX_SQS_BATCH = config.get("MAX_SQS_BATCH_SIZE");
const DEFAULT_SOCKET_TIMEOUT = config.get("timeout.DEFAULT_SOCKET_TIMEOUT");
const DEFAULT_REQUEST_TIMEOUT = config.get("timeout.DEFAULT_REQUEST_TIMEOUT");
const DEFAULT_CONNECTION_TIMEOUT = config.get("timeout.DEFAULT_CONNECTION_TIMEOUT");
const TIMEOUT_EXCEPTIONS = config.get("TIMEOUT_EXCEPTIONS");

const sqs = new SQSClient({
    requestHandler: new NodeHttpHandler({
      connectionTimeout: DEFAULT_CONNECTION_TIMEOUT,
      requestTimeout: DEFAULT_REQUEST_TIMEOUT,
      socketTimeout: DEFAULT_SOCKET_TIMEOUT,
    }),
});

async function writeMessagesToQueue(immediateActions, context, destinationQueueUrl) {
  console.log("Starting writeMessagesToQueue");

  while (immediateActions.length > 0 && !isTimeToLeave(context)) {
    console.log(
      "Proceeding to send " +
        immediateActions.length +
        " messages to " +
        destinationQueueUrl
    );

    let splicedActionsArray = immediateActions.splice(0, MAX_SQS_BATCH);
    let actionsToSendMapped = getMappedMessageToSend(splicedActionsArray);

    const command = createBatchCommand(actionsToSendMapped, destinationQueueUrl);

    try {
      checkMandatoryInformation(actionsToSendMapped, destinationQueueUrl);
      const response = await sqs.send(command);
      console.log("Sent message response: %j", response);
      
      if (response.Failed && response.Failed.length > 0) {
        return checkAndReturnFailedAction(splicedActionsArray, response);
      }
    }
    catch (exceptions) {
      console.error("Error in send sqs message ", exceptions);
      console.log("Stringfy exception ", JSON.stringify(exceptions));
      if (exceptions.name && TIMEOUT_EXCEPTIONS.includes(exceptions.name)) {
        console.warn(
          "[ACTION_ENQUEUER]",
          "Timeout detected for:",
          JSON.stringify(actionsToSendMapped)
        );
        let actionTimeoutDlqUrl = config.get("ACTION_TIMEOUT_ERROR_DLQ_URL");
        writeMessagesToSqsWithoutReturnFailed(actionsToSendMapped, actionTimeoutDlqUrl);

      }else{
        console.info('Generic exception in SQS send message, need to reschedule');
        return splicedActionsArray; //Non si conoscono gli item specifici falliti, viene restituito tutto il batch
      }
    }
  }
  
  console.log("Ending writeMessagesToQueue with arrayActionNotSended length", immediateActions.length);
  return immediateActions;
}

function checkMandatoryInformation(actionsToSendMapped, destinationQueueUrl){
  if (!destinationQueueUrl){
    console.debug("Destination SQS queue cannot be empty need to reschedule actions ", JSON.stringify(actionsToSendMapped));
    throw new Error("No SQS queue supplied");
  }
  
  if (!actionsToSendMapped || actionsToSendMapped.length === 0){
    console.debug("message to send cannot be empty need to reschedule actions ", JSON.stringify(actionsToSendMapped));
    throw new Error("message to send cannot be empty");
  }
}

function checkAndReturnFailedAction(splicedActionsArray, response){
  console.log('There is an error in sending message ', response.Failed)
  let failedActionArray = [];
  
  console.log('Start find error in actionToSend ',JSON.stringify(splicedActionsArray) )

  splicedActionsArray.forEach((element) => {
    if (
      response.Failed.filter((currFailed) => currFailed.Id == element.Id)
        .length !== 0
    )
    failedActionArray.push(element); //Se fallisce nella put
  });

  return failedActionArray; //viene restituito l'array delle action Fallite
}

function getMappedMessageToSend(splicedActionsArray){
  let actionsToSendMapped = [];
  splicedActionsArray.forEach(function (action) {
    let messageToSend = mapActionToQueueMessage(action);
    action.Id = messageToSend.Id;
    actionsToSendMapped.push(messageToSend);
  });
  return actionsToSendMapped;
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


async function writeMessagesToSqsWithoutReturnFailed(actionsToSendMapped, destinationQueueUrl) {
  console.log(
    "writeMessagesToWithoutReturnFailed " +
      immediateActions.length +
      " messages to " +
      destinationQueueUrl
  );
  const command = createBatchCommand(actionsToSendMapped, destinationQueueUrl);

  try {
    checkMandatoryInformation(actionsToSendMapped, destinationQueueUrl);
    const response = await sqs.send(command);
    console.log("Sent message response: %j", response);
    
    if (response.Failed && response.Failed.length > 0) {
      console.error(
        "[ACTION_ENQUEUER]",
        "Insert action failed:",
        JSON.stringify(response.Failed)
      );
    }
  }
  catch (exceptions) {
    console.error("Error in send sqs message ", exceptions);
    console.error(
      "[ACTION_ENQUEUER]",
      "Insert action failed:",
      JSON.stringify(response.Failed)
    );
  }  
}

function createBatchCommand(actionsToSendMapped, destinationQueueUrl){
  const input = {
    Entries: actionsToSendMapped,
    QueueUrl: destinationQueueUrl,
  };
  console.log("Sending batch message: %j", input);
  return new SendMessageBatchCommand(input);
}

module.exports = { writeMessagesToQueue };