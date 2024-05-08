const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

const eventHandler = proxyquire.noCallThru().load("../app/eventHandler.js", {
    "./utils/utils.js": {
      isRecordToSend: () =>{
        return true;
      },
      isFutureAction: () => {
        return true;
      }
    },
    "./sqs/writeToSqs.js": {
        writeMessagesToQueue: () =>{
            let notSendedImmediateActions = [];
            return notSendedImmediateActions;
        }
    },
    "./dynamo/writeToDynamo.js": {
        writeMessagesToDynamo: (futureActions, context) =>{
            let notSendedFutureActions = [];
            return notSendedFutureActions;
        }
    },
});

describe("eventHandler tests", function () {
  it("first-test", async () => {
    const event = mockEvent;
    const context = contextMock;
    const res = await eventHandler.handleEvent(event, context);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });
});

const mockEvent = {
    Records: [
      {
        kinesis: {
            kinesisSchemaVersion: "1.0",
            partitionKey: "B125405A111F418CE1F1E8D872D1E778",
            sequenceNumber: "49651198519313044168944402159308296562011022530273345538",
            data: "eyJhd3NSZWdpb24iOiJldS1jZW50cmFsLTEiLCJldmVudElEIjoiNTI4NDIxMzYtMzQ2YS00NTM0LWFiMzAtNDljMGY5YTIxMGE2IiwiZXZlbnROYW1lIjoiSU5TRVJUIiwidXNlcklkZW50aXR5IjpudWxsLCJyZWNvcmRGb3JtYXQiOiJhcHBsaWNhdGlvbi9qc29uIiwidGFibGVOYW1lIjoicG9jLXBuLUFjdGlvbiIsImR5bmFtb2RiIjp7IkFwcHJveGltYXRlQ3JlYXRpb25EYXRlVGltZSI6MTcxNTE3NTcyOTkyOSwiS2V5cyI6eyJhY3Rpb25JZCI6eyJTIjoiVGVzdF8zNDI1Mi0xMTEzNy0yMDI0XzA0XzE1XzEzNTMtMzI1Mi00MjUyLTIwMjQzOC0zLThfNGZiMDQ4MWUtMDVjOC00Y2JlLTlkNGMtZDM5NmU3OTNkY2NkYXNkYXNkYXNkIn19LCJOZXdJbWFnZSI6eyJpdW4iOnsiUyI6IjEzNTMtMzI1Mi00MjUyLTIwMjQzOC0zLTgifSwidGltZWxpbmVJZCI6eyJTIjoidGltZWxpbmVJZDEifSwibm90QmVmb3JlIjp7IlMiOiIyMDI0LTA0LTE1VDIwOjM3OjU0LjI1MloifSwicmVjaXBpZW50SW5kZXgiOnsiUyI6InJlY2lwaWVudEluZGV4MSJ9LCJpbnNlcnRBY3Rpb25UaW1lc3RhbXAiOnsiUyI6IjIwMjQtMDQtMTVUMjA6Mzg6NTQuMjcxWiJ9LCJ0aW1lc2xvdCI6eyJTIjoiMjAyNC0wNC0xNVQyMDozNyJ9LCJ0eXBlIjp7IlMiOiJ0eXBlMSJ9LCJhY3Rpb25JZCI6eyJTIjoiVGVzdF8zNDI1Mi0xMTEzNy0yMDI0XzA0XzE1XzEzNTMtMzI1Mi00MjUyLTIwMjQzOC0zLThfNGZiMDQ4MWUtMDVjOC00Y2JlLTlkNGMtZDM5NmU3OTNkY2NkYXNkYXNkYXNkIn19LCJTaXplQnl0ZXMiOjQwM30sImV2ZW50U291cmNlIjoiYXdzOmR5bmFtb2RiIn0=",
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
    ]
}

contextMock = {
    awsRequestId: '60ed6b20-85df-4ee3-af82-d835f281b915',
    getRemainingTimeInMillis: getRemainingTimeInMillis()
}

function getRemainingTimeInMillis(){
    return 1000000;
}