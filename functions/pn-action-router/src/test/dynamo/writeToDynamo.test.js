const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire").noPreserveCache();
const config = require("config");
const FUTURE_ACTION_TABLE_NAME = config.get("FUTURE_ACTION_TABLE_NAME");

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
    const isTimeToLeaveStub = sinon.stub().returns(false);

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

    const DynamoDBClientStub = sinon.stub();
    const batchWriteStub = sinon.stub();
    
    DynamoDBClientStub.prototype.batchWrite = batchWriteStub;
    batchWriteStub.returns(mockResponse);

    const writeToDynamo = proxyquire("../../app/dynamo/writeToDynamo.js", {
      "@aws-sdk/lib-dynamodb": {
        DynamoDBClient: DynamoDBClientStub,
        DynamoDBDocument: {
          from: () => ({
            batchWrite: (params) =>{
              console.log(params)
              params.RequestItems[FUTURE_ACTION_TABLE_NAME].forEach((element) => {
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

    let itemNotSended = arrayActionToStore[0];
    
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
                 Item: itemNotSended
              }
           }
        ]
     }
    }

    DynamoDBClientStub.prototype.batchWrite = batchWriteStub;
    batchWriteStub.returns(mockResponse);

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
        isTimeToLeave: () =>{
          return false;
        },
      },
    });

    const res = await writeToDynamo.writeMessagesToDynamo(arrayActionToStore, contextMock);
    
    expect(res).to.not.be.null;
    expect(res.length).equal(1);
    expect(res[0].actionId).equal(itemNotSended.actionId)
  });
});