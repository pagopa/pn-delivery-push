const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();
const { describe, it } = require('mocha');

const futureActionDate = getFutureActionDate();
const immediateAction = getImmediateActionDate();

const queue1 = 'coda1';
const queue2 = 'coda2';

let sendedActionToQueue = [];
let sendedActionToDynamo = [];

const eventHandler = proxyquire.noCallThru().load("../app/eventHandler.js", {
    "./utils/utils.js": {
      isRecordToSend: () =>{
        return true;
      },
      isFutureAction: (notBefore) => {
        var date = new Date();
        let isoDateNow = date.toISOString();
        return isoDateNow < notBefore;
      }
    },
    "./utils/getActionDestination.js": {
      getActionDestination: (action) => {
        return action.destinationQueueName; //viene inserita in fase di test la destination
      }
    },

    "./sqs/writeToSqs.js": {
        writeMessagesToQueue: (actionsToSend, context, destinationQueueName) =>{
            for (var i = 0; i < actionsToSend.length; i++) {
              let action = actionsToSend[i];
              let copiedAction = Object.assign({}, action);
              copiedAction.destinationQueueName = destinationQueueName;
              sendedActionToQueue.push(copiedAction);
            }
            let notSendedImmediateActions = [];
            return notSendedImmediateActions;
        }
    },
    "./dynamo/writeToDynamo.js": {
        writeMessagesToDynamo: (futureActions, context) =>{
            sendedActionToDynamo = sendedActionToDynamo.concat(futureActions);            
            let notSendedFutureActions = [];
            return notSendedFutureActions;
        }
    },
});

describe("eventHandler tests", function () {
  it.only("first-test", async () => {
    //GIVEN
    sendedActionToQueue = [];
    sendedActionToDynamo = [];

    let futureAction1Data = getData('futureAction1', futureActionDate, null, true);
    let immAction1Data = getData('immediateAction1', immediateAction, queue1, false);
    let immAction2Data = getData('immediateAction2', immediateAction, queue1, false);
    let futureAction2Data = getData('futureAction2', futureActionDate, null, true);
    let immAction3Data = getData('immediateAction3', immediateAction, queue2, false);
    let immAction4Data = getData('immediateAction4', immediateAction, queue2, false);
    let immAction5Data = getData('immediateAction5', immediateAction, queue1, false);
    let futureAction3Data = getData('futureAction3', futureActionDate, null, true);
    let futureAction4Data = getData('futureAction4', futureActionDate, null, true);

    let arrayInsertedData = [futureAction1Data,immAction1Data,immAction2Data,futureAction2Data,immAction3Data,immAction4Data,
      immAction5Data,futureAction3Data,futureAction4Data ];
    
    const mockEvent = {
        Records: [
          getKinesisRecord(getBase64Record(futureAction1Data)),
          getKinesisRecord(getBase64Record(immAction1Data)),
          getKinesisRecord(getBase64Record(immAction2Data)),
          getKinesisRecord(getBase64Record(futureAction2Data)),
          getKinesisRecord(getBase64Record(immAction3Data)),
          getKinesisRecord(getBase64Record(immAction4Data)),
          getKinesisRecord(getBase64Record(immAction5Data)),
          getKinesisRecord(getBase64Record(futureAction3Data)),
          getKinesisRecord(getBase64Record(futureAction4Data)),
        ]
    }

    const event = mockEvent;
    const context = contextMock;

    //WHEN
    const res = await eventHandler.handleEvent(event, context);

    //THEN
    expect(res).deep.equals({
      batchItemFailures: [],
    });

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
          actionSendToQueue.destinationQueueName == actionData.destinationQueueName 
        );
        expect(findInSendToQueue.length).deep.equals(1);
      }
    }

  });
});

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

function getData(actionId, notBefore, destinationQueueName, isFutureAction){
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
             "S": 'ActionType'
          },
          "actionId":{
             "S": actionId
          },
          "destinationQueueName":{ //ONLY FOR TEST SCOPE
            "S": destinationQueueName 
         }
       },
       "SizeBytes":407
    },
    "eventSource":"aws:dynamodb"
  };
}
function getKinesisRecord(data){
  return {
    kinesis: {
        kinesisSchemaVersion: "1.0",
        partitionKey: "B125405A111F418CE1F1E8D872D1E778",
        sequenceNumber: "49651198519313044168944402159308296562011022530273345538",
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


function decodeBase64(encodedRecord) {
  var decodedString = Buffer.from(encodedRecord, "base64").toString();
  let decodedJson = JSON.parse(decodedString);
  console.log("decodedJson", decodedJson);
  return decodedJson;
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

function setElementInMap(map, actionData){
  map.set(actionData.dynamodb.NewImage.actionId, actionData.dynamodb.NewImage);
}