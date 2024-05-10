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

const fakeActions2 = [
  {
    notBefore: "2024-05-12T13:00:00.384Z",
    iun: "iunFake1",
    timeSlot: "2024-05-12T13:00:00",
    actionId: "actionIdFake1",
  },

  {
    notBefore: "2024-05-12T13:00:00.384Z",
    iun: "iunFake2",
    timeSlot: "2024-05-12T13:00:00",
    actionId: "actionIdFake2",
  },
];

const sqsConfig = { endpoint: "fakeEndpoint" };

describe("test SQS putMessage", () => {
  it("SQS configuration Error", async () => {
    const mockSQSClient = {
      send: async () => ({}), // Mock per un successo
    };
    const lambda = proxyquire.noCallThru().load("../app/sqsFunctions.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      },
    });

    const result = await lambda.putMessages(
      {},
      fakeActions.slice(),
      isTimedOut
    );
    expect(result).to.be.an("array").that.is.not.empty;
  });

  it("SQS success", async () => {
    const mockSQSClient = {
      send: async () => ({}), // Mock per un successo
    };

    const lambda = proxyquire.noCallThru().load("../app/sqsFunctions.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      },
    });

    const result = await lambda.putMessages(
      sqsConfig,
      fakeActions.slice(),
      isTimedOut
    );
    expect(result).to.be.an("array").that.is.empty;
  });

  it("SQS sendMessageTimeout", async () => {
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

    const lambda = proxyquire.noCallThru().load("../app/sqsFunctions.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      },
    });

    const result = await lambda.putMessages(
      sqsConfig,
      fakeActions.map((x) => Object.assign({}, x)),
      isTimedOut
    );

    expect(result).to.be.an("array").that.is.empty;
  });

  it("SQS sendMessage Exception ", async () => {
    const mockSQSClient = {
      send: async () => {
        class GenericToException extends Error {
          constructor(e) {
            super(`${e.message}`);
            this.name = "GenericError";
          }
        }
        throw new GenericToException("SQS send error");
      },
    };

    const lambda = proxyquire.noCallThru().load("../app/sqsFunctions.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      },
    });

    const result = await lambda.putMessages(
      sqsConfig,
      fakeActions.map((x) => Object.assign({}, x)),
      isTimedOut
    );

    expect(result).to.be.an("array").that.is.not.empty;
  });

  it("SQS sendMessage Error ", async () => {
    const mockSQSClient = {
      send: async () => ({
        Failed: [
          {
            Id: "1",
            Code: "SomeErrorCode",
            Message: "Errore nell'invio del messaggio 1",
          },
        ],
      }),
    };
    let current = 2;
    const lambda = proxyquire.noCallThru().load("../app/sqsFunctions.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      },
      uuid: {
        v4: () => current++,
      },
    });

    let myFakeActions = fakeActions2.map((x) => Object.assign({}, x));
    const result = await lambda.putMessages(
      sqsConfig,
      myFakeActions,
      isTimedOut
    );
    expect(result).to.be.an("array").that.is.not.empty;
    expect(result.length).equal(1);
    expect(result[0].iun).equal("iunFake2");
  });
});
