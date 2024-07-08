/* eslint-disable no-unused-vars */
const { expect } = require("chai");
const { describe, it } = require("mocha");
const proxyquire = require("proxyquire").noPreserveCache();
const {
  parseISO,
  nextTimeSlot,
  isAfter,
  toString: dateToString,
} = require("../app/timeHelper");

const STEP_SIZE = 10;
const SLOT_1 = "2024-05-22T11:58";
const SLOT_2 = "2024-05-22T11:59";

const chunk = (items, step) => {
  if (step == undefined) {
    step = 0;
  }
  return items.slice(step * STEP_SIZE, (step + 1) * STEP_SIZE);
};

const testOverride = (
  lastWorkedTimeslot,
  read,
  deletion,
  testDir,
  currSlot
) => {
  return {
    "./timeHelper.js": {
      actTime: () => parseISO("2024-05-22T12:00"),
      parseISO,
      nextTimeSlot,
      isAfter,
      toString: (d) => dateToString(d),
    },

    "./dynamoFunctions.js": {
      getLastTimeSlotWorked: async (_unused1, _unused2) => lastWorkedTimeslot,
      setLastTimeSlotWorked: async (_unused1, _unused2, newTime) =>
        (lastWorkedTimeslot = newTime),
      getActionsByTimeSlot: async (
        _unused1,
        { timeSlot, _sub_unused1, _sub_unused2 },
        _unused2
      ) => {
        let dataSet;
        switch (timeSlot) {
          case SLOT_1:
            dataSet = require(`${testDir}/11_58.json`);
            currSlot = SLOT_1;
            break;
          case SLOT_2:
            dataSet = require(`${testDir}/11_59.json`);
            currSlot = SLOT_2;
            break;
          default:
            console.log("ERROR", JSON.stringify(timeSlot));
        }
        const testData = chunk(dataSet, read[currSlot]);
        if (testData.length == 0) {
          return { items: testData };
        } else {
          read[currSlot] ? read[currSlot]++ : (read[currSlot] = 1);
          return { items: testData, lastEvaluatedKey: 1 };
        }
      },
      batchDelete: async (_unused1, _unused2, _unused3) => {
        deletion[currSlot] != undefined
          ? deletion[currSlot]++
          : (deletion[currSlot] = 1);
        return { operationResult: true, discarded: 0};
      },
    },
  };
};

describe("eventHandler tests", () => {
  it("two slot with data partition", async () => {
    let lastWorkedTimeslot = "2024-05-22T11:57";
    let currSlot;
    let deletion = {};
    let read = {};
    const lambda = proxyquire
      .noCallThru()
      .load(
        "../app/eventHandler.js",
        testOverride(
          lastWorkedTimeslot,
          read,
          deletion,
          "./testData/test-twoRepeats",
          currSlot
        )
      );
    const result = await lambda.handleEvent(null, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.not.be.null;
    expect(result.statusCode).to.be.eq(200);
    expect(result).to.have.property("body");
    expect(result.body).to.be.eq(JSON.stringify({}));
    expect(read[SLOT_1]).to.be.equal(5);
    expect(read[SLOT_2]).to.be.equal(5);
    expect(deletion[SLOT_1]).to.be.equal(5);
    expect(deletion[SLOT_2]).to.be.equal(5);
  });
  it("first slot with data partition", async () => {
    let lastWorkedTimeslot = "2024-05-22T11:57";
    let currSlot;
    let deletion = {};
    let read = {};
    const lambda = proxyquire
      .noCallThru()
      .load(
        "../app/eventHandler.js",
        testOverride(
          lastWorkedTimeslot,
          read,
          deletion,
          "./testData/test-onlyOneRepeat",
          currSlot
        )
      );
    const result = await lambda.handleEvent(null, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.not.be.null;
    expect(result.statusCode).to.be.eq(200);
    expect(result).to.have.property("body");
    expect(result.body).to.be.eq(JSON.stringify({}));
    expect(read[SLOT_1]).to.be.equal(5);
    expect(read[SLOT_2]).to.be.equal(1);
    expect(deletion[SLOT_1]).to.be.equal(5);
    expect(deletion[SLOT_2]).to.be.equal(1);
  });
  it("empty slot", async () => {
    let lastWorkedTimeslot = "2024-05-22T11:57";
    let currSlot;
    let deletion = {};
    let read = {};
    const lambda = proxyquire
      .noCallThru()
      .load(
        "../app/eventHandler.js",
        testOverride(
          lastWorkedTimeslot,
          read,
          deletion,
          "./testData/empty",
          currSlot
        )
      );
    const result = await lambda.handleEvent(null, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.not.be.null;
    expect(result.statusCode).to.be.eq(200);
    expect(result).to.have.property("body");
    expect(result.body).to.be.eq(JSON.stringify({}));
    expect(read[SLOT_1]).to.be.undefined;
    expect(read[SLOT_2]).to.be.undefined;
    expect(deletion[SLOT_1]).to.be.undefined;
    expect(deletion[SLOT_2]).to.be.undefined;
  });
});
