const { isRecordToSend, isFutureAction } = require("./utils/utils.js");
const { getActionDestination } = require("./utils/getActionDestination.js");
const { writeMessagesToQueue } = require("./sqs/writeToSqs.js");
const { writeMessagesToDynamo } = require("./dynamo/writeToDynamo.js");
const { unmarshall } = require("@aws-sdk/util-dynamodb");

const handleEvent = async (event, context) => {
  let notSendedActions = await startHandleEvent(event, context);

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
}

async function startHandleEvent (event, context) {
  console.log(JSON.stringify(event, null, 2));

  let actionToSend = [];
  let lastActionType = undefined; //future or immediate
  let lastDestinationQueue = undefined;

  for (var i = 0; i < event.Records.length; i++) {
    let record = event.Records[i];
    let sequenceNumber = record.kinesis.sequenceNumber;
    let decodedRecord = decodeBase64(record.kinesis.data);

    if (isRecordToSend(decodedRecord)) {
      const action = mapMessageFromKinesisToAction(decodedRecord,sequenceNumber);
      let currentActionType;
      if(isFutureAction(action.notBefore)){
        currentActionType = 'FutureAction';
        if(lastActionType != currentActionType && actionToSend.length > 0){
          const notSended = await writeMessagesToQueue(actionToSend, context, lastDestinationQueue);
          if (notSended != 0) {
            return notSended;
          }
          actionToSend = [];
          lastDestinationQueue = undefined; //probabilmente non serve
        }
      }else{
        currentActionType = 'ImmediateAction';
        if(lastActionType != currentActionType && actionToSend.length > 0){
          const notSended = await writeMessagesToDynamo(actionToSend,context);
          if (notSended != 0) {
            return notSended;
          }
          actionToSend = [];
        }
        let currentDestinationQueue = getActionDestination(action);
        if (currentDestinationQueue != lastDestinationQueue && actionToSend.length > 0) {
          const notSended = await writeMessagesToQueue(actionToSend, context, lastDestinationQueue);
          if (notSended != 0) {
            return notSended;
          }
          actionToSend = [];
        }
        lastDestinationQueue = currentDestinationQueue;
      }

      actionToSend.push(action);
      lastActionType = currentActionType;
    }
  }

  let notSended = [];
  if (actionToSend.length > 0) {
    let lastAction = actionToSend[actionToSend.length - 1];
    if(isFutureAction(lastAction.notBefore)){
      notSended = await writeMessagesToDynamo(actionToSend,context);
    }else{
      notSended = await writeMessagesToQueue(actionToSend, context, lastDestinationQueue);
    }
  }
  
  return notSended;
};

function mapMessageFromKinesisToAction(record, sequenceNumber) {
  console.log("il record e", record);
  let action = record.dynamodb.NewImage;
  console.log("action", action);
  const regularAction = unmarshall(action);
  regularAction.kinesisSeqNo = sequenceNumber;
  console.log("regularAction", regularAction);
  return regularAction;
}

function decodeBase64(encodedRecord) {
  var decodedString = Buffer.from(encodedRecord, "base64").toString();
  let decodedJson = JSON.parse(decodedString);
  console.log("decodedJson", decodedJson);
  return decodedJson;
}

module.exports = { handleEvent };
