const { isRecordToSend, isFutureAction } = require("./utils/utils.js");
const { getActionDestination } = require("./utils/getActionDestination.js");
const { writeMessagesToQueue } = require("./sqs/writeToSqs.js");
const { writeMessagesToDynamo } = require("./dynamo/writeToDynamo.js");
const { insideWorkingWindow, getWorkingTime } = require("./utils/workingTimeUtils.js");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const { ActionUtils } = require("pn-action-common");
const config = require("config");

async function handleEvent(event, context){
  let notSendedActions = await startHandleEvent(event, context);
  console.log('startHandleEvent finished')
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

async function startHandleEvent(event, context) {
  console.log('startHandleEvent Start')
  console.log(JSON.stringify(event, null, 2));
  let actionToSend = [];
  let lastActionType = undefined; //future or immediate
  let lastDestinationQueue = undefined;
  const workingTime = await getWorkingTime();
  console.log('workingTime for action is ', workingTime)

  for (var i = 0; i < event.Records.length; i++) {
    let record = event.Records[i];
    let sequenceNumber = record.kinesis.sequenceNumber;
    let decodedRecord = decodeBase64(record.kinesis.data);

    if (isRecordToSend(decodedRecord)) {

      console.log('the record is to send ', decodedRecord );

      const action = mapMessageFromKinesisToAction(decodedRecord,sequenceNumber);
      if(insideWorkingWindow(action, workingTime.start, workingTime.end)){
        
        console.log('start handling action ', action.actionId)
        let currentActionType;
        if(isFutureAction(action.notBefore)){
          console.log('Start to check future action ', action.actionId)
          currentActionType = 'FutureAction';
          if(lastActionType != currentActionType && actionToSend.length > 0){
            const notSended = await writeMessagesToQueue(actionToSend, context, lastDestinationQueue);
            console.log('notSended returned from queue 1 is ', notSended)
            if (notSended != 0) {
              return notSended;
            }
            actionToSend = [];
            lastDestinationQueue = undefined; //probabilmente non serve
          }
        }else{
          console.log('Start to check immediate action ', action.actionId)
          currentActionType = 'ImmediateAction';
          if(lastActionType != currentActionType && actionToSend.length > 0){
            const notSended = await writeMessagesToDynamo(actionToSend,context);
            console.log('notSended returned from dynamo is ', notSended)
            if (notSended != 0) {
              console.log("there are 'Not sended item', need to return")
              return notSended;
            }
            console.log("All items are sent correctly")
            actionToSend = [];
          }
          let currentDestinationQueue = await ActionUtils.getQueueUrl(
            action?.type,
            action?.details,
            config.get("ACTION_MAP_ENV_VARIABLE"),
            config.get("QUEUE_ENDPOINTS_ENV_VARIABLE")
          );
          if (currentDestinationQueue != lastDestinationQueue && actionToSend.length > 0) {
            console.log('currentDestinationQueue is different from lastDestination, need to send message ')
  
            const notSended = await writeMessagesToQueue(actionToSend, context, lastDestinationQueue);
            console.log('notSended returned from queue 2 is ', notSended)
            if (notSended != 0) {
              return notSended;
            }
            actionToSend = [];
          }
          lastDestinationQueue = currentDestinationQueue;
        }
        
        actionToSend.push(action);
        lastActionType = currentActionType;
        console.log('Handling action completed ', action.actionId)  
      }else{
        console.log('Action is not in working windows ', action.notBefore)
      }
    }else{
      console.log('The record is not to send ', decodedRecord)
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
