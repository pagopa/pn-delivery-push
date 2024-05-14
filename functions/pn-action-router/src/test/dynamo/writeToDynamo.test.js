const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire").noPreserveCache();

describe("DynamoDB tests", function () {
  const arrayActionToStore = [
    {
      timeslot: "2021-09-23T10:00",
      actionId: "actionId1",
      notBefore: "2021-09-23T10:00:00.000Z",
      recipientIndex: 0,
      type: "NOTIFICATION_CANCELLATION",
      timelineId: "notification_cancellation_request.IUN_XLDW-MQYJ-WUKA-202302-A-1",
      iun: "XLDW-MQYJ-WUKA-202302-A-1",
      details: undefined
    },
    {
      timeslot: "2021-09-23T10:00",
      actionId: "actionId2",
      notBefore: "2021-09-23T10:00:00.000Z",
      recipientIndex: 0,
      type: "NOTIFICATION_CANCELLATION",
      timelineId: "notification_cancellation_request.IUN_XLDW-MQYJ-WUKA-202302-A-1",
      iun: "XLDW-MQYJ-WUKA-202302-A-1",
      details: undefined
    },
    {
      timeslot: "2021-09-23T10:00",
      actionId: "actionId3",
      notBefore: "2021-09-23T10:00:00.000Z",
      recipientIndex: 0,
      type: "NOTIFICATION_CANCELLATION",
      timelineId: "notification_cancellation_request.IUN_XLDW-MQYJ-WUKA-202302-A-1",
      iun: "XLDW-MQYJ-WUKA-202302-A-1",
      details: undefined
    },
  ];

  let startDate = new Date();

  const contextMock = {
    awsRequestId: '60ed6b20-85df-4ee3-af82-d835f281b915',
    startDate: startDate
  };

  it("test persistEvents", async () => {
    
    // Stubbing isTimeToLeave function
    const isTimeToLeaveStub = sinon.stub().returns(false); // Modifica questo valore a seconda del comportamento che desideri simulare

    let mockResponse= {
      '$metadata': {
        httpStatusCode: 200,
        requestId: 'TSS3T9B161QG0USKFSDIHM94FJVV4KQNSO5AEMVJF66Q9ASUAAJG',
        extendedRequestId: undefined,
        cfId: undefined,
        attempts: 1,
        totalRetryDelay: 0
      },
      UnprocessedItems: {}
    }
    
    let writedActionArray = [];

    // Stubbing DynamoDBClient and DynamoDBDocumentClient
    const DynamoDBClientStub = sinon.stub();
    const batchWriteStub = sinon.stub();
    
    // Assegna la funzione di stubbing al metodo batchWrite di DynamoDBDocumentClient
    DynamoDBClientStub.prototype.batchWrite = batchWriteStub;
    batchWriteStub.returns(mockResponse);

    // Carica il modulo con il proxyquire e sostituisci le dipendenze necessarie con gli stub creati sopra
    const writeToDynamo = proxyquire("../../app/dynamo/writeToDynamo.js", {
      "@aws-sdk/lib-dynamodb": {
        DynamoDBClient: DynamoDBClientStub,
        DynamoDBDocument: {
          from: () => ({
            batchWrite: (params) =>{
              console.log(params)
              params.RequestItems.pnFutureAction.forEach((element) => {
                let action = element.PutRequest.Item;
                writedActionArray.push(action);
              });
              
              return mockResponse;
            }
          })
        }
      },
      "../utils/utils.js": {
        isTimeToLeave: isTimeToLeaveStub
      }
    });

    const res = await writeToDynamo.writeMessagesToDynamo(arrayActionToStore.slice(), contextMock);
    
    expect(res).to.not.be.null;
    expect(res.length).equal(0);

    arrayActionToStore.forEach((element) => {
      const findInSendToDynamo = writedActionArray.filter( actionWrited => 
        actionWrited.actionId == element.actionId
      );
      expect(findInSendToDynamo.length).deep.equals(1);
    });

  });

  it("test persistEvents Error", async () => {
    // Stubbing DynamoDBClient and DynamoDBDocumentClient
    const DynamoDBClientStub = sinon.stub();
    const batchWriteStub = sinon.stub();
    let mockResponse= {
      '$metadata': {
        httpStatusCode: 200,
        requestId: 'TSS3T9B161QG0USKFSDIHM94FJVV4KQNSO5AEMVJF66Q9ASUAAJG',
        extendedRequestId: undefined,
        cfId: undefined,
        attempts: 1,
        totalRetryDelay: 0
      },
      UnprocessedItems: {
        "pn-FutureAction":[
           {
              PutRequest:{
                 Item: arrayActionToStore[0]
              }
           }
        ]
     }
    }

    // Assegna la funzione di stubbing al metodo batchWrite di DynamoDBDocumentClient
    DynamoDBClientStub.prototype.batchWrite = batchWriteStub;
    batchWriteStub.returns(mockResponse);

    // Carica il modulo con il proxyquire e sostituisci le dipendenze necessarie con gli stub creati sopra
    const writeToDynamo = proxyquire("../../app/dynamo/writeToDynamo.js", {
      "@aws-sdk/lib-dynamodb": {
        DynamoDBClient: DynamoDBClientStub,
        DynamoDBDocument: {
          from: () => ({
            batchWrite: batchWriteStub
          })
        }
      },
      "../utils/utils.js": {
        isTimeToLeave: (context) =>{
          console.log('Context is ', context);
          var nowDate   = new Date();
          var seconds = (nowDate.getTime() - context.startDate.getTime()) / 1000;
          if(seconds > 1){
            return true;
          }
          return false;
        },
      },
    });

    const res = await writeToDynamo.writeMessagesToDynamo(arrayActionToStore, contextMock);
    
    expect(res).to.not.be.null;
    expect(res.length).equal(1);
  });
});