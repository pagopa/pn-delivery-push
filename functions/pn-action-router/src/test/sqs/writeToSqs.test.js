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
    kinesisSeqNo: "1"
  },
];

const fakeActions2 = [
  {
    notBefore: "2024-05-12T13:00:00.384Z",
    iun: "iunFake1",
    timeSlot: "2024-05-12T13:00:00",
    actionId: "actionIdFake",
    kinesisSeqNo: "1"
  },
  {
    notBefore: "2024-05-12T13:00:00.384Z",
    iun: "iunFake2",
    timeSlot: "2024-05-12T13:00:00",
    actionId: "actionIdFake",
    kinesisSeqNo: "2"
  },
  {
    notBefore: "2024-05-12T13:00:00.384Z",
    iun: "iunFake3",
    timeSlot: "2024-05-12T13:00:00",
    actionId: "actionIdFake",
    kinesisSeqNo: "3"
  }
];

contextMock = {
  awsRequestId: '60ed6b20-85df-4ee3-af82-d835f281b915'
}

describe("test SQS putMessage", () => {
  it("SQS configuration Error", async () => {
    let destinationQueueUrl = undefined;

    const mockSQSClient = {
      send: async () => ({}), // Mock per un successo
    };
    const writeToSqs = proxyquire.noCallThru().load("../../app/sqs/writeToSqs.js", {
      "../utils/utils.js": {
        isTimeToLeave: () =>{
          return false;
        },
      },
      uuid: {
        v4: () => 'testUuid',
      },
  
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

  it("SQS success", async () => {
    let destinationQueueUrl = 'Test-destination-queue';
    const mockSQSClient = {
      send: async () => ({}), // Mock per un successo
    };

    const writeToSqs = proxyquire.noCallThru().load("../../app/sqs/writeToSqs.js", {
      "../utils/utils.js": {
        isTimeToLeave: () =>{
          return false;
        },
      },
      uuid: {
        v4: () => 'testUuid',
      },
  
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
    expect(result).to.be.an("array").that.is.empty;
  });

  it("SQS sendMessageTimeout", async () => {
    let destinationQueueUrl = 'Test-destination-queue';

    const mockSQSClient = {
      send: async () => {
        class TimeoutException extends Error {
          constructor(e) {
            super(`${e.message}`);
            this.name = "TimeoutError";
          }
        }
        throw new TimeoutException("Timeout SQS send error");
      },
    };

    const writeToSqs = proxyquire.noCallThru().load("../../app/sqs/writeToSqs.js", {
      "../utils/utils.js": {
        isTimeToLeave: () =>{
          return false;
        },
      },
      uuid: {
        v4: () => 'testUuid',
      },
  
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

    expect(result).to.be.an("array").that.is.empty;
  });


  it("SQS failedResponse", async () => {
    let currentUuid = 0;
    let indexElementToFail = 0;
    let destinationQueueUrl = 'Test-destination-queue';

    const mockSQSClient = {
      send: async () => ({
        Failed: [
          {
            Id: indexElementToFail,
            Code: "SomeErrorCode",
            Message: "Errore nell'invio del messaggio",
          },
        ],
      })
    };

    const writeToSqs = proxyquire.noCallThru().load("../../app/sqs/writeToSqs.js", {
      "../utils/utils.js": {
        isTimeToLeave: () =>{
          return false;
        },
      },
      uuid: {
        v4: () => currentUuid++,
      },
  
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
      fakeActions2.slice(),
      contextMock,
      destinationQueueUrl
    )
    
    console.log('result returned is', result);
    console.log('fakeActions2 ', fakeActions2[indexElementToFail])
    
    let failedElement = result[0];
    expect(failedElement.kinesisSeqNo).deep.equals(fakeActions2[indexElementToFail].kinesisSeqNo);
  });
  
});