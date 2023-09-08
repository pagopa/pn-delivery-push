const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

describe("event handler tests", function () {

  it("test Ok", async () => {
    const event = {};

    const mockSQSClient = {
      send: async () => ({}) // Mock per un successo
    };

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      },
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{}];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return [{ test: 1 }];
        },
      },
    });


    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test errore nella send", async () => {
    const event = {};

    const mockSQSClient = {
      send: async () => ({
        Failed: [
          {
            Id: "message-1",
            Code: "SomeErrorCode",
            Message: "Errore nell'invio del messaggio 1",
          },
        ],
      }),
    };

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      },
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{payload: '1', kinesisSeqNumber: 'test'}];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return [{ test: 1 }];
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [{itemIdentifier: 'message-1'}],
    });
  });

  it("test exception nella send", async () => {
    const event = {};

    const mockSQSClient = {
      send: async () => {
        throw new Error("Simulated SQS Error");
      },
    };

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "@aws-sdk/client-sqs": {
        SQSClient: class {
          constructor() {
            return mockSQSClient;
          }
        },
        SendMessageBatchCommand: class {},
      },
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{payload: '1', kinesisSeqNumber: 'test'}];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return [{ test: 1 }];
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [{itemIdentifier: 'test'}],
    });
  });

  it("test no kinesis data", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return [{ test: 1 }];
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test no data to persist", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{ test: 1 }];
        },
      },
      "./lib/eventMapper.js": {
        mapEvents: () => {
          return [];
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });


});
