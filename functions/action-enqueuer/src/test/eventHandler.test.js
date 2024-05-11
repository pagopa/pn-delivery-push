const { expect } = require("chai");
const { describe, it } = require("mocha");
const proxyquire = require("proxyquire").noPreserveCache();

describe("eventHandler test ", function () {
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
        putMessages: (sqsConfig, actions, isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getCurrentDestination: (action) => {
          const splitted = action.actionId.split("-");
          return splitted.length == 2 ? splitted[1] : splitted;
        },
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
        putMessages: (sqsConfig, actions, isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getCurrentDestination: (action) => {
          const splitted = action.actionId.split("-");
          return splitted.length == 2 ? splitted[1] : splitted;
        },
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
        putMessages: (sqsConfig, actions, isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getCurrentDestination: (action) => {
          const splitted = action.actionId.split("-");
          return splitted.length == 2 ? splitted[1] : splitted;
        },
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
        putMessages: (sqsConfig, actions, isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getCurrentDestination: (action) => {
          const splitted = action.actionId.split("-");
          return splitted.length == 2 ? splitted[1] : splitted;
        },
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
        putMessages: (sqsConfig, actions, isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getCurrentDestination: (action) => {
          const splitted = action.actionId.split("-");
          return splitted.length == 2 ? splitted[1] : splitted;
        },
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

    console.log("DATA ENCODED", testData);

    let invokedCount = 0;

    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./sqsFunctions.js": {
        putMessages: (sqsConfig, actions, isTimedOut) => {
          invokedCount++;
          return [];
        },
      },
      "./utils.js": {
        getCurrentDestination: (action) => {
          const splitted = action.actionId.split("-");
          return splitted.length == 2 ? splitted[1] : splitted;
        },
      },
    });

    const result = await lambda.handleEvent(testData, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.be.not.null;
    expect(result.batchItemFailures).to.be.empty;
    expect(invokedCount).equal(3);
  });
});
