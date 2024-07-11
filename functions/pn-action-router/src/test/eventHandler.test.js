const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();
const { describe, it } = require('mocha');
const config = require("config");

const futureActionDate = getFutureActionDate();
const immediateAction = getImmediateActionDate();

const queue1 = 'coda1';
const queue2 = 'coda2';

let sendedActionToQueue = [];
let sendedActionToDynamo = [];
let isRecordToSend = true;

const eventHandler = proxyquire.noCallThru().load("../app/eventHandler.js", {
    "./utils/utils.js": {
      isRecordToSend: () =>{
        return isRecordToSend;
      },
      isFutureAction: (notBefore) => {
        var date = new Date();
        let isoDateNow = date.toISOString();
        return isoDateNow < notBefore;
      }
    },

    "./sqs/writeToSqs.js": {
        writeMessagesToQueue: async (actionsToSend, context, destinationQueueUrl) =>{
            let notSendedImmediateActions = [];

            //Vengono aggiunte le azioni inviate all'array delle azioni inviate in coda
            for (var i = 0; i < actionsToSend.length; i++) {

              let action = actionsToSend[i];
              if(! action.actionToFail){
                console.log('[TEST] action is not to fail ', action)

                let copiedAction = Object.assign({}, action);
                copiedAction.destinationQueueUrl = destinationQueueUrl;
                sendedActionToQueue.push(copiedAction);  
              }else{
                //viene simulato il fallimento nell'invio di un action alla coda
                console.log('[TEST] action to fail ', action)
                notSendedImmediateActions = actionsToSend
              }
            }
            
            return notSendedImmediateActions;
        }
    },
    "./dynamo/writeToDynamo.js": {
        writeMessagesToDynamo: async (futureActions, context) =>{
            //Simula risposta positiva, notSendedAction vuoto []           
            let notSendedFutureActions = [];

            for (var i = 0; i < futureActions.length; i++) {
              let futureAction = futureActions[i];
              if(! futureAction.actionToFail){
                sendedActionToDynamo.push(futureAction);  
              }else{
                //viene simulato il fallimento nell'invio di un action alla coda
                notSendedFutureActions = actionsToSend
              }
            }

            return notSendedFutureActions;
        }
    },
});

describe("eventHandlerTest", function () {
  let queueAction1Url = "https://sqs.eu-south-1.amazonaws.com/830192246553/pn-delivery_push_actions1";
  let queueAction2Url = "https://sqs.eu-south-1.amazonaws.com/830192246553/pn-delivery_push_actions2";

  before(() => {
    process.env[config.get("ACTION_MAP_ENV_VARIABLE")] =
      '{"DOCUMENT_CREATION_RESPONSE_SENDER_ACK":"actionId-queue2","DOCUMENT_CREATION_RESPONSE":"actionId-queue2","NOTIFICATION_CREATION":"actionId-queue2","NOTIFICATION_VALIDATION":"actionId-queue1"}';
    
    process.env[config.get("QUEUE_ENDPOINTS_ENV_VARIABLE")] =
      '{"actionId-queue2":"https://sqs.eu-south-1.amazonaws.com/830192246553/pn-delivery_push_actions2", "actionId-queue1":"https://sqs.eu-south-1.amazonaws.com/830192246553/pn-delivery_push_actions1"}';
  });

  after(() => {
    delete process.env[[config.get("ACTION_MAP_ENV_VARIABLE")]];
    delete process.env[[config.get("QUEUE_ENDPOINTS_ENV_VARIABLE")]];
  });

  it("complete-test", async () => {
    //GIVEN
    initializeMockData()
    let isActionToFail = false;

    //Vengono definite le action da sottoporre al test
    let futureAction1Data = getData('futureAction1', futureActionDate, 'REFINEMENT', true, isActionToFail, null);
    let immAction1Data = getData('immediateAction1', immediateAction, 'NOTIFICATION_VALIDATION', false, isActionToFail, queueAction1Url);
    let immAction2Data = getData('immediateAction2', immediateAction, 'NOTIFICATION_VALIDATION', false, isActionToFail, queueAction1Url);
    let futureAction2Data = getData('futureAction2', futureActionDate, 'DIGITAL_DELIVERY', true, isActionToFail, null);
    let immAction3Data = getData('immediateAction3', immediateAction, 'NOTIFICATION_CREATION', false, isActionToFail, queueAction2Url);
    let immAction4Data = getData('immediateAction4', immediateAction, 'NOTIFICATION_CREATION', false, isActionToFail, queueAction2Url);
    let immAction5Data = getData('immediateAction5', immediateAction, 'NOTIFICATION_VALIDATION', false, isActionToFail, queueAction1Url);
    let futureAction3Data = getData('futureAction3', futureActionDate, 'NOTIFICATION_VIEWED', true, isActionToFail, null);
    let futureAction4Data = getData('futureAction4', futureActionDate, 'NOTIFICATION_PAID', true, isActionToFail, null);

    //Viene definito l'array delle action inserite
    let arrayInsertedData = [futureAction1Data,immAction1Data,immAction2Data,futureAction2Data,immAction3Data,immAction4Data,
      immAction5Data,futureAction3Data,futureAction4Data ];
    
    //Viene strutturato l'evento di mock così come lo definisce kinesis
    const mockEvent = {
        Records: [
          getKinesisRecord(getBase64Record(futureAction1Data), futureAction1Data.eventID),
          getKinesisRecord(getBase64Record(immAction1Data), immAction1Data.eventID),
          getKinesisRecord(getBase64Record(immAction2Data), immAction2Data.eventID),
          getKinesisRecord(getBase64Record(futureAction2Data), futureAction2Data.eventID),
          getKinesisRecord(getBase64Record(immAction3Data), immAction3Data.eventID),
          getKinesisRecord(getBase64Record(immAction4Data), immAction4Data.eventID),
          getKinesisRecord(getBase64Record(immAction5Data), immAction5Data.eventID),
          getKinesisRecord(getBase64Record(futureAction3Data), futureAction3Data.eventID),
          getKinesisRecord(getBase64Record(futureAction4Data), futureAction4Data.eventID),
        ]
    }

    //WHEN
    const res = await eventHandler.handleEvent(mockEvent, contextMock);

    //THEN

    //Viene verificato che non ci siano item per la quale la put su dynamo o in coda sia fallita
    expect(res).deep.equals({
      batchItemFailures: [],
    });
    
    let actionToQueueNameMap;
    let queueNameMapToQueueUrlMap;
  
    //Viene verificato che le azioni siano state inserite in maniera corretta in futureAction piuttosto che in coda (e nella coda giusta)
    checkAllEventSentToCorrectDestination(arrayInsertedData, sendedActionToDynamo, sendedActionToQueue, actionToQueueNameMap, queueNameMapToQueueUrlMap);

  });

  it("one-item-test", async () => {
    //GIVEN
    initializeMockData();
    isRecordToSend = true;

    let isActionToFail = false;
    

    //Vengono definite le action da sottoporre al test
    let futureAction1Data = getData('futureAction1', futureActionDate, 'REFINEMENT', true, isActionToFail, null);

    //Viene definito l'array delle action inserite
    let arrayInsertedData = [futureAction1Data];
    
    //Viene strutturato l'evento di mock così come lo definisce kinesis
    const mockEvent = {
        Records: [
          getKinesisRecord(getBase64Record(futureAction1Data))
        ]
    }

    //WHEN
    const res = await eventHandler.handleEvent(mockEvent, contextMock);

    //THEN
    //Viene verificato che non ci siano item per la quale la put su dynamo o in coda sia fallita
    expect(res).deep.equals({
      batchItemFailures: [],
    });

    //Viene verificato che le azioni siano state inserite in maniera corretta in futureAction piuttosto che in coda (e nella coda giusta)
    checkAllEventSentToCorrectDestination(arrayInsertedData, sendedActionToDynamo, sendedActionToQueue);
  });

  it("no-record-to-send", async () => {
    //GIVEN
    initializeMockData()
    isRecordToSend = false;
    let isActionToFail = false;

    //Vengono definite le action da sottoporre al test
    let futureAction1Data = getData('futureAction1', futureActionDate, 'REFINEMENT', true, isActionToFail, null);
    let immAction1Data = getData('immediateAction1', immediateAction, 'NOTIFICATION_CREATION', false, isActionToFail, queueAction2Url);
    let immAction2Data = getData('immediateAction2', immediateAction, 'NOTIFICATION_CREATION', false, isActionToFail, queueAction2Url);

    //Viene strutturato l'evento di mock così come lo definisce kinesis
    const mockEvent = {
        Records: [
          getKinesisRecord(getBase64Record(futureAction1Data), futureAction1Data.eventID),
          getKinesisRecord(getBase64Record(immAction1Data), immAction1Data.eventID),
          getKinesisRecord(getBase64Record(immAction2Data), immAction2Data.eventID)
        ]
    }

    //WHEN
    const res = await eventHandler.handleEvent(mockEvent, contextMock);

    //THEN

    //Viene verificato che non ci siano item per la quale la put su dynamo o in coda sia fallita
    expect(res).deep.equals({
      batchItemFailures: [],
    });

    //Viene che nessuna azione sia stata inviata in coda
    expect(sendedActionToQueue.length).deep.equals(0);
    //Viene che nessuna azione sia stata inviata in dynamo
    expect(sendedActionToDynamo.length).deep.equals(0);

  });

  it("action-to-fail", async () => {
    //GIVEN
    initializeMockData()
    let isActionToFail = false;

    //Vengono definite le action da sottoporre al test
    let futureAction1Data = getData('futureAction1', futureActionDate, 'REFINEMENT', true, isActionToFail, null);
    let immAction1Data = getData('immediateAction1', immediateAction, 'NOTIFICATION_VALIDATION', false, isActionToFail, queueAction1Url);
    let immAction2Data = getData('immediateAction2', immediateAction, 'NOTIFICATION_VALIDATION', false, true, queueAction1Url);
    let futureAction2Data = getData('futureAction2', futureActionDate, 'REFINEMENT', true, isActionToFail, null);
    let immAction3Data = getData('immediateAction3', immediateAction, 'NOTIFICATION_CREATION', false, isActionToFail, queueAction2Url);
 
    //Viene definito l'array delle action inserite
    let arrayInsertedData = [futureAction1Data ];
    
    //Viene strutturato l'evento di mock così come lo definisce kinesis
    const mockEvent = {
        Records: [
          getKinesisRecord(getBase64Record(futureAction1Data), futureAction1Data.eventID),
          getKinesisRecord(getBase64Record(immAction1Data), immAction1Data.eventID),
          getKinesisRecord(getBase64Record(immAction2Data), immAction2Data.eventID),
          getKinesisRecord(getBase64Record(futureAction2Data), futureAction2Data.eventID),
          getKinesisRecord(getBase64Record(immAction3Data), immAction3Data.eventID),
        ]
    }

    //WHEN
    const res = await eventHandler.handleEvent(mockEvent, contextMock);

    //THEN
    
    //Ci si aspetta che siano fallite le azioni immAction1Data e immAction2Data, questo perchè a fallire è stata immediateAction2 dunque tutto il batch relativo

    //Viene che l'array dei fallimneti sia lungo 2
    expect(res.batchItemFailures.length).deep.equals(2);
    
    //viene controllato che sia fallita l'action immAction1Data
    const foundImmAction1Data = res.batchItemFailures.filter( itemFailure => 
      itemFailure.itemIdentifier == immAction1Data.eventID
    );
    expect(foundImmAction1Data.length).deep.equals(1);

    //viene controllato che sia fallita l'action immAction2Data
    const foundImmAction2Data = res.batchItemFailures.filter( itemFailure => 
      itemFailure.itemIdentifier == immAction2Data.eventID
    );
    expect(foundImmAction2Data.length).deep.equals(1);

    //Viene verificato che sia stata inviata la sola azione attesa futureAction1Data
    checkAllEventSentToCorrectDestination(arrayInsertedData, sendedActionToDynamo, sendedActionToQueue);
  });
});

function checkAllEventSentToCorrectDestination(arrayInsertedData, sendedActionToDynamo, sendedActionToQueue, actionToQueueNameMap, queueNameMapToQueueUrlMap){
  for (var i = 0; i < arrayInsertedData.length; i++) {
    let actionData = arrayInsertedData[i];
    let actionDataInfo = actionData.dynamodb.NewImage;
    if(actionData.isFutureAction){
      //Viene verificato che l'azione futura sia stata effettivamente inviata verso dynamo
      const findInSendToDynamo = sendedActionToDynamo.filter( actionSendToDynamo => 
        actionSendToDynamo.actionId == actionDataInfo.actionId.S
      );
      expect(findInSendToDynamo.length).deep.equals(1);
    }else{
      //Viene verificato che l'azione futura sia stata effettivamente inviata verso la specifica coda di destinazione
      const findInSendToQueue = sendedActionToQueue.filter( actionSendToQueue => 
        actionSendToQueue.actionId == actionDataInfo.actionId.S 
        &&
        actionSendToQueue.destinationQueueUrl == actionDataInfo.expectedDestinationQueueUrl.S
      );
      expect(findInSendToQueue.length).deep.equals(1);
    }
  }
}

contextMock = {
    awsRequestId: '60ed6b20-85df-4ee3-af82-d835f281b915',
    getRemainingTimeInMillis: getRemainingTimeInMillis()
}

function getBase64Record(data){
  return btoa(JSON.stringify(data, null, 2))
}
function getRemainingTimeInMillis(){
    return 1000000;
}

function getData(actionId, notBefore, actionType, isFutureAction, isActionToFail, expectedDestinationQueueUrl){
  return {
    "isFutureAction": isFutureAction, //ONLY FOR TEST SCOPE
    "eventID": actionId + '_eventId',
    "eventName":"INSERT",
    "recordFormat":"application/json",
    "tableName": 'pn-action',
    "dynamodb":{
       "ApproximateCreationDateTime":1715262731416,
       "Keys":{
          "actionId":{
             "S": actionId
          }
       },
       "NewImage":{
          "iun":{
             "S":"1347-7238-8238-202428-2-8"
          },
          "timelineId":{
             "S":"timelineId1"
          },
          "notBefore":{
             "S": notBefore
          },
          "recipientIndex":{
             "S":"recipientIndex1"
          },
          "insertActionTimestamp":{
             "S":"2024-04-15T20:37:58.929Z"
          },
          "timeslot":{
             "S":"2024-04-15T20:59"
          },
          "type":{
             "S": actionType
          },
          "actionId":{
             "S": actionId
          },
          "details":{
            "S": 'details' 
         },
         "actionToFail":{ //ONLY FOR TEST SCOPE
          "S": isActionToFail 
         },
         "expectedDestinationQueueUrl":{ //ONLY FOR TEST SCOPE
          "S": expectedDestinationQueueUrl 
         }
       },
       "SizeBytes":407
    },
    "eventSource":"aws:dynamodb"
  };
}
function getKinesisRecord(data, itemIdentifier){
  return {
    kinesis: {
        kinesisSchemaVersion: "1.0",
        partitionKey: "B125405A111F418CE1F1E8D872D1E778",
        sequenceNumber: itemIdentifier,
        data: data,
        approximateArrivalTimestamp: 1715175730.148
    },
    eventSource: 'aws:kinesis',
    eventVersion: '1.0',
    eventID: 'shardId-000000000000:49651198519313044168944402159308296562011022530273345538',
    eventName: 'aws:kinesis:record',
    invokeIdentityArn: 'arn:aws:iam::830192246553:role/PocWrite-PocPnActionRouterLambdaRole-Cymsxig2wtSQ',
    awsRegion: 'eu-central-1',
    eventSourceARN: 'arn:aws:kinesis:eu-central-1:830192246553:stream/poc-pn-cdc'
  }
}

function getFutureActionDate(){
  var date = new Date();
  date.setDate(date.getDate() + 1);
  return date.toISOString();
}

function getImmediateActionDate(){
  var date = new Date();
  date.setDate(date.getDate() - 1);
  return date.toISOString();
}

function initializeMockData(){
  sendedActionToQueue = [];
  sendedActionToDynamo = [];
  isRecordToSend = true;
}