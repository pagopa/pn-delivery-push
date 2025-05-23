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

const {
  InvalidItemException
} = require("../app/exceptions");


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
  timeslot,
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
      getLastTimeSlotWorked: async (_unused1, _unused2) => timeslot.lastWorkedTimeslot,
      setLastTimeSlotWorked: async (_unused1, _unused2, newTime) =>
        timeslot.lastWorkedTimeslot = newTime,
      getActionsByTimeSlot: async (
        _unused1,
        timeSlot,
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
    let timeslot = {lastWorkedTimeslot: "2024-05-22T11:57"};
    let currSlot;
    let deletion = {};
    let read = {};
    const lambda = proxyquire
      .noCallThru()
      .load(
        "../app/eventHandler.js",
        testOverride(
          timeslot,
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
    expect(timeslot.lastWorkedTimeslot).to.be.equal("2024-05-22T11:59");
  });

  it("first slot with data partition", async () => {
    let timeslot = {lastWorkedTimeslot: "2024-05-22T11:57"};
    let currSlot;
    let deletion = {};
    let read = {};
    const lambda = proxyquire
      .noCallThru()
      .load(
        "../app/eventHandler.js",
        testOverride(
          timeslot,
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
    expect(timeslot.lastWorkedTimeslot).to.be.equal("2024-05-22T11:59");
  });

  it("two slot with some elements to discard", async () => {
    let timeslot = {lastWorkedTimeslot: "2024-05-22T11:57"};
    let currSlot;
    let deletion = {};
    let read = {};


    const opsOverride = testOverride(
      timeslot,
      read,
      deletion,
      "./testData/test-with-discarded",
      currSlot
    );
    opsOverride["./dynamoFunctions.js"].batchDelete = async (_unused1, items, _unused3) => {
      deletion[items[0].timeslot] != undefined
        ? deletion[items[0].timeslot]++
        : (deletion[items[0].timeslot] = 1);
        if (deletion[items[0].timeslot] == 1) return { operationResult: true, discarded:1};
      return { operationResult: true, discarded: 0};
    };

    const lambda = proxyquire
      .noCallThru()
      .load(
        "../app/eventHandler.js",
        opsOverride
      );
    const result = await lambda.handleEvent(null, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.not.be.null;
    expect(result.statusCode).to.be.eq(500);
    expect(result).to.have.property("body");
    let errorResult = JSON.parse(result.body);
 
    expect(errorResult).to.have.property("error");
    expect(errorResult.error).to.have.property("name");
    let bOEx = new InvalidItemException();
    expect(errorResult.error.name).to.be.eq(bOEx.name);
    expect(read[SLOT_2]).to.be.undefined;

    expect(read[SLOT_1]).to.be.equal(5);
    expect(deletion[SLOT_1]).to.be.equal(5);
    expect(deletion[SLOT_2]).to.be.undefined;

    expect(timeslot.lastWorkedTimeslot).to.be.equal("2024-05-22T11:57");
  });


  it("two slot with one slot less in deletion", async () => {
    let timeslot = {lastWorkedTimeslot: "2024-05-22T11:57"};
    let currSlot;
    let deletion = {};
    let read = {};


    const opsOverride = testOverride(
      timeslot,
      read,
      deletion,
      "./testData/test-with-discarded-one-slot-less",
      currSlot
    );
    opsOverride["./dynamoFunctions.js"].batchDelete = async (_unused1, items, _unused3) => {
      deletion[items[0].timeslot] != undefined
        ? deletion[items[0].timeslot]++
        : (deletion[items[0].timeslot] = 1);
        if (deletion[items[0].timeslot] == 1) return { operationResult: true, discarded:1};
      return { operationResult: true, discarded: 0};
    };

    const lambda = proxyquire
      .noCallThru()
      .load(
        "../app/eventHandler.js",
        opsOverride
      );
    const result = await lambda.handleEvent(null, {
      getRemainingTimeInMillis: () => 10000000000,
    });
    expect(result).to.not.be.null;
    expect(result.statusCode).to.be.eq(500);
    expect(result).to.have.property("body");
    let errorResult = JSON.parse(result.body);
 
    expect(errorResult).to.have.property("error");
    expect(errorResult.error).to.have.property("name");
    let bOEx = new InvalidItemException();
    expect(errorResult.error.name).to.be.eq(bOEx.name);
    expect(read[SLOT_2]).to.be.undefined;

    expect(read[SLOT_1]).to.be.equal(5);
    expect(deletion[SLOT_1]).to.be.equal(5);
    expect(deletion[SLOT_2]).to.be.undefined;
    expect(timeslot.lastWorkedTimeslot).to.be.equal("2024-05-22T11:57");
  });

  it("empty slot", async () => {
    let timeslot = {lastWorkedTimeslot: "2024-05-22T11:57"};
    let currSlot;
    let deletion = {};
    let read = {};
    const lambda = proxyquire
      .noCallThru()
      .load(
        "../app/eventHandler.js",
        testOverride(
          timeslot,
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
    expect(timeslot.lastWorkedTimeslot).to.be.equal("2024-05-22T11:59");

  });
});
