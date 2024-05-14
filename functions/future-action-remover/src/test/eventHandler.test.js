const { expect } = require("chai");
const { describe, it, before, after } = require("mocha");
const proxyquire = require("proxyquire").noPreserveCache();

const {
  nextTimeSlot,
  isAfter,
  parseISO,
  actTime,
  toString: dateToString,
} = require("./timeHelper.js");

const { generateKoResponse, generateOkResponse } = require("./responses.js");
const {
  getLastTimeSlotWorked,
  setLastTimeSlotWorked,
  getActionsByTimeSlot,
  batchDelete,
} = require("./dynamoFunctions.js");

describe("test insideWorkingWindow", () => {
  it({});
});
