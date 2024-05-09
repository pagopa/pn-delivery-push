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

    const result = await lambda.putMessages({}, fakeActions, isTimedOut);
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

    const result = await lambda.putMessages(sqsConfig, fakeActions, isTimedOut);
    expect(result).to.be.an("array").that.is.empty;
  });

  it("SQS sendMessageTimeout", async () => {
    const mockSQSClient = {
      send: async () => {
        class TimeoutException extends Error {
          constructor(e) {
            super(`${e.message}`);
            this.name = "GenericError";
          }
        }
        throw new TimeoutException("Timeout SQS send error");
      },
    };

    // const mockSQSClient = {
    //   send: async () => ({
    //     Failed: [
    //       {
    //         Id: "message-1",
    //         Code: "SomeErrorCode",
    //         Message: "Errore nell'invio del messaggio 1",
    //       },
    //     ],
    //   }),
    // };

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

    const result = await lambda.putMessages(sqsConfig, fakeActions, isTimedOut);
    expect(result).to.be.an("array").that.is.empty;
  });

  it("SQS sendMessage Exception ", async () => {
    const mockSQSClient = {
      send: async () => {
        class GenericException extends Error {
          constructor(e) {
            super(`${e.message}`);
            this.name = "GenericError";
          }
        }
        throw new GenericException(" SQS send error");
      },
    };

    // const mockSQSClient = {
    //   send: async () => ({
    //     Failed: [
    //       {
    //         Id: "message-1",
    //         Code: "SomeErrorCode",
    //         Message: "Errore nell'invio del messaggio 1",
    //       },
    //     ],
    //   }),
    // };

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

    const result = await lambda.putMessages(sqsConfig, fakeActions, isTimedOut);
    expect(result).to.be.an("array").that.is.not.empty;
  });
});
