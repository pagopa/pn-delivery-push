const { expect } = require("chai");
const { describe, it } = require("mocha");
const proxyquire = require("proxyquire").noPreserveCache();

const isTimedOut = () => false;
const fakeActions = [
  {
    notBefore: "2024-05-12T13:00:00.384Z",
    iun: "iunFake",
    timeSlot: "2024-05-12T13:00:00",
    actionId: "actionIdFake",
  },
];

contextMock = {
  awsRequestId: '60ed6b20-85df-4ee3-af82-d835f281b915',
  getRemainingTimeInMillis: getRemainingTimeInMillis()
}

describe("test SQS putMessage", () => {
  it.only("SQS configuration Error", async () => {
    let destinationQueueUrl = undefined;

    const mockSQSClient = {
      send: async () => ({}), // Mock per un successo
    };
    const writeToSqs = proxyquire.noCallThru().load("../../app/sqs/writeToSqs.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      },
    });

    const result = await writeToSqs.writeMessagesToQueue(
      fakeActions,
      contextMock,
      destinationQueueUrl
    );
    expect(result).to.be.an("array").that.is.not.empty;
  });

});

function getRemainingTimeInMillis(){
    return 1000000;
}