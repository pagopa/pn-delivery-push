const { expect } = require("chai");
const proxyquire = require("proxyquire").noPreserveCache();

// event handler tests
describe("event handler tests", function () {
  it("test Ok with persistence enabled", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
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
      "./lib/repository.js": {
        persistEvents: async () => {
          return {
            insertions: 1,
            errors: [],
          };
        },
      },
      "./lib/utils.js": {
        isPersistenceEnabled: () => true,
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test Ok with action-manager", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
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
      "./lib/repository.js": {
        persistEvents: async () => {
          return {
            insertions: 1,
            errors: [],
          };
        },
      },
      "./lib/utils.js": {
        isPersistenceEnabled: () => false,
      },
      "./lib/client.js": {
        insertAction: async () => {
          return {
            insertions: 1,
            errors: [],
          };
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test no Kinesis data", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [];
        },
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [],
    });
  });

  it("test no mapped data", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./lib/kinesis.js": {
        extractKinesisData: () => {
          return [{}];
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

  it("test with persist errors", async () => {
    const event = {};

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
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
      "./lib/repository.js": {
        persistEvents: async () => {
          return {
            insertions: 0,
            errors: [{ kinesisSeqNumber: "4950" }],
          };
        },
      },
      "./lib/utils.js": {
        isPersistenceEnabled: () => true,
      },
    });

    const res = await lambda.handleEvent(event);
    expect(res).deep.equals({
      batchItemFailures: [{ itemIdentifier: "4950" }],
    });
  });
});
