const { isRecordToSend, isFutureAction } = require("./utils/utils.js");
const { getActionDestination } = require("./utils/getActionDestination.js");
const { writeMessagesToQueue } = require("./sqs/writeToSqs.js");
const { writeMessagesToDynamo } = require("./dynamo/writeToDynamo.js");
const { insideWorkingWindow, getWorkingTime } = require("./utils/workingTimeUtils.js");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const { ActionUtils } = require("pn-action-common");
const config = require("config");

async function handleEvent(event, context){
  console.log('Start handling event')

  let notSendedActions = await startHandleEvent(event, context);
  const result = {
    batchItemFailures: [],
  };
  if (notSendedActions.length !== 0) {
    notSendedActions.forEach((element) =>
      result.batchItemFailures.push({ itemIdentifier: element.kinesisSeqNo })
    );
  }
  console.info("Handling message completed. Result returned to kinesis is ", result);
  return result;
}

async function startHandleEvent(event, context) {
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
        console.log('start handling specific action ', action.actionId)

        let currentActionType;
        if(isFutureAction(action.notBefore)){
          //Se si tratta di un azione futura ...
          console.info('Start to check future action ', action.actionId)
          currentActionType = 'FutureAction';
          if(lastActionType != currentActionType && actionToSend.length > 0){
            //... e l'ultima azione inviata ha un tipo differente (immediata) ...
            const notSended = await writeMessagesToQueue(actionToSend, context, lastDestinationQueue);
            //... . Si procede ad inviare il batch delle azioni immediate alla coda definita per l'utlima azione (che sappiamo essere uguale a quella di tutto il batch)
            if (notSended != 0) {
              console.warn("there are 'Not sended item', need to return. Not sended item", notSended)
              return notSended;
            }
            console.log("All batch items are sent correctly")
            actionToSend = [];
            lastDestinationQueue = undefined; //Viene settato per pulizia
          }
        }else{
          //Se si tratta di un azione immediata ...

          console.info('Start to check immediate action ', action.actionId)
          currentActionType = 'ImmediateAction';
          if(lastActionType != currentActionType && actionToSend.length > 0){
            //... e l'ultima azione inviata ha un tipo differente (futura) ...
            const notSended = await writeMessagesToDynamo(actionToSend,context);
            //... si procede ad inviare il batch delle azioni future
            if (notSended != 0) {
              console.warn("there are 'Not sended item', need to return. Not sended item", notSended)
              return notSended;
            }
            console.log("All batch items are sent correctly")
            actionToSend = [];
          }
          let currentDestinationQueue = await ActionUtils.getQueueUrl(
            action?.type,
            action?.details,
            config.get("ACTION_MAP_ENV_VARIABLE"),
            config.get("QUEUE_ENDPOINTS_ENV_VARIABLE")
          );
          //Viene ottenuta la coda destinazione per l'azione corrente ...
          if (currentDestinationQueue != lastDestinationQueue && actionToSend.length > 0) {
            //... Se la coda di destinazione corrente è diversa da quella dell'ultima action...
            console.log('currentDestinationQueue is different from lastDestination, need to send message ')
  
            const notSended = await writeMessagesToQueue(actionToSend, context, lastDestinationQueue);
            //... Si può procede ad inviare il batch di messaggi alla lastDestinationQueue (che siamo sicuri essere uguale per tutto il batch di messaggi)...
            if (notSended != 0) {
              console.warn("there are 'Not sended item', need to return. Not sended item", notSended)
              return notSended;
            }
            actionToSend = [];
          }
          lastDestinationQueue = currentDestinationQueue;
          console.log('New lastDestinationQueue queue is ', lastDestinationQueue)
        }
        
        actionToSend.push(action);
        lastActionType = currentActionType;
        console.log('New lastActionType queue is ', lastDestinationQueue)
        console.info('Handling action completed ', action.actionId)
      }else{
        console.info('Action is not in working windows ', action.notBefore)
      }
    }else{
      console.info('The record is not to send ', decodedRecord)
    }
  }

  let notSended = [];
  if (actionToSend.length > 0) {
    //Qui vengono prese le ultima action rimaste nell'Array, una volta terminato il for ed inviate
    console.log('There are the last asctions to send')

    let lastAction = actionToSend[actionToSend.length - 1]; //Viene preso l'ultimo elemento e verificata la category, che siamo sicuri essere uguale a quella di tutti gli altri elementi
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
