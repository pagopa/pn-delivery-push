const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

describe("event handler tests", function () {

  process.env = Object.assign(process.env, {
    START_READ_STREAM_TIMESTAMP: "1999-01-01T00:00:00Z",
    STOP_READ_STREAM_TIMESTAMP: "2099-01-01T00:00:00Z"
  });

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

  it("event in between flag interval", async () => {
    const testData = require("./kinesis.event.example.json");

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

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
  });

  it("event before flag interval", async () => {
    process.env = Object.assign(process.env, {
      START_READ_STREAM_TIMESTAMP: "2098-01-01T00:00:00Z",
      STOP_READ_STREAM_TIMESTAMP: "2099-01-01T00:00:00Z"
    });
    const testData = require("./kinesis.event.example.json");

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

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
  });

  it("event after flag interval", async () => {
    process.env = Object.assign(process.env, {
      START_READ_STREAM_TIMESTAMP: "1998-01-01T00:00:00Z",
      STOP_READ_STREAM_TIMESTAMP: "1999-01-01T00:00:00Z"
    });
    const testData = require("./kinesis.event.example.json");

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

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
  });

});
