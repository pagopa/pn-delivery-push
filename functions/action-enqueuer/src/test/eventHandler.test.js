/* eslint-disable no-unused-vars */
const { expect } = require("chai");
const { describe, it, before, after } = require("mocha");
const proxyquire = require("proxyquire").noPreserveCache();
const { getWorkingTime } = require("../app/workingTimeUtils");
const config = require("config");

describe("eventHandler test ", function () {
  before(() => {
    process.env[config.get("ACTION_MAP_ENV_VARIABLE")] =
      '{"DOCUMENT_CREATION_RESPONSE_SENDER_ACK":"actionId-queue2","DOCUMENT_CREATION_RESPONSE":"actionId-queue2","NOTIFICATION_CREATION":"actionId-queue2","NOTIFICATION_VALIDATION":"actionId-queue1"}';
  });

  after(() => {
    delete process.env[[config.get("ACTION_MAP_ENV_VARIABLE")]];
  });

  it("send record in oneQueue - one element", async () => {
    const testData = require("./streamData/one.json");

    testData.Records.map((record) => {
      const dataEncoded = Buffer.from(
        JSON.stringify(record.kinesis.data),
        "ascii"
      ).toString("base64");
      record.kinesis.data = dataEncoded;
      return record;
    });

    console.log("DATA ENCODED", testData);

    let invokedCount = 0;

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./sqsFunctions.js": {
        putMessages: (_sqsConfig, _actions, _isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getQueueName: (actionType, _details, _envVarName) => actionType,
      },
    });

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
    expect(invokedCount).equal(1);
  });
  it("send record in oneQueue - two element", async () => {
    const testData = require("./streamData/two-oneQueue.json");

    testData.Records.map((record) => {
      const dataEncoded = Buffer.from(
        JSON.stringify(record.kinesis.data),
        "ascii"
      ).toString("base64");
      record.kinesis.data = dataEncoded;
      return record;
    });

    console.log("DATA ENCODED", testData);

    let invokedCount = 0;

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./sqsFunctions.js": {
        putMessages: (_sqsConfig, _actions, _isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getQueueName: (actionType, _details, _envVarName) => actionType,
      },
    });

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
    expect(invokedCount).equal(1);
  });
  it("send record in two Queue - two element", async () => {
    const testData = require("./streamData/two-twoQueue.json");

    testData.Records.map((record) => {
      const dataEncoded = Buffer.from(
        JSON.stringify(record.kinesis.data),
        "ascii"
      ).toString("base64");
      record.kinesis.data = dataEncoded;
      return record;
    });

    console.log("DATA ENCODED", testData);

    let invokedCount = 0;

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./sqsFunctions.js": {
        putMessages: (_sqsConfig, _actions, _isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getQueueName: (actionType, _details, _envVarName) => actionType,
      },
    });

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
    expect(invokedCount).equal(2);
  });
  it("send record in two Queue - three element - 1", async () => {
    const testData = require("./streamData/three-twoQueue-permutation1.json");

    testData.Records.map((record) => {
      const dataEncoded = Buffer.from(
        JSON.stringify(record.kinesis.data),
        "ascii"
      ).toString("base64");
      record.kinesis.data = dataEncoded;
      return record;
    });

    console.log("DATA ENCODED", testData);

    let invokedCount = 0;

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./sqsFunctions.js": {
        // eslint-disable-next-line no-unused-vars
        putMessages: (_sqsConfig, _actions, _isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getQueueName: (actionType, _details, _envVarName) => actionType,
      },
    });

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
    expect(invokedCount).equal(2);
  });
  it("send record in two Queue - three element - 2", async () => {
    const testData = require("./streamData/three-twoQueue-permutation2.json");

    testData.Records.map((record) => {
      const dataEncoded = Buffer.from(
        JSON.stringify(record.kinesis.data),
        "ascii"
      ).toString("base64");
      record.kinesis.data = dataEncoded;
      return record;
    });

    console.log("DATA ENCODED", testData);

    let invokedCount = 0;

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./sqsFunctions.js": {
        putMessages: (_sqsConfig, _actions, _isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getQueueName: (actionType, _details, _envVarName) => actionType,
      },
    });

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
    expect(invokedCount).equal(2);
  });

  it("send record in two Queue - three element - 3", async () => {
    const testData = require("./streamData/three-twoQueue-permutation3.json");

    testData.Records.map((record) => {
      const dataEncoded = Buffer.from(
        JSON.stringify(record.kinesis.data),
        "ascii"
      ).toString("base64");
      record.kinesis.data = dataEncoded;
      return record;
    });

    let invokedCount = 0;

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./sqsFunctions.js": {
        putMessages: (_sqsConfig, _actions, _isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getQueueName: (actionType, _details, _envVarName) => actionType,
      },
    });

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
    expect(invokedCount).equal(3);
  });

  it("filter with working window", async () => {
    const testData = require("./streamData/one_of_three_working_window.json");

    testData.Records.map((record) => {
      const dataEncoded = Buffer.from(
        JSON.stringify(record.kinesis.data),
        "ascii"
      ).toString("base64");
      record.kinesis.data = dataEncoded;
      return record;
    });

    let invokedCount = 0;

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./sqsFunctions.js": {
        putMessages: (_sqsConfig, _actions, _isTimedOut) => {
          invokedCount += _actions.length;
          return [];
        },
      },
      "./utils.js": {
        getQueueName: (actionType, _details, _envVarName) => actionType,
      },
      "./workingTimeUtils": {
        insideWorkingWindow: (action, start, end) => {
          return action.actionId === "ACCEPT";
        },
        getWorkingTime: getWorkingTime,
      },
    });

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
    expect(invokedCount).equal(1);
  });
});
