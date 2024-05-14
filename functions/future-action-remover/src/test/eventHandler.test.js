/* eslint-disable no-unused-vars */
const { expect } = require("chai");
const { describe, it, before, after } = require("mocha");
const proxyquire = require("proxyquire").noPreserveCache();
const { parseISO, nextTimeSlot, isAfter } = require("../app/timeHelper");

describe("eventHandler tests", () => {
  it("first test", async () => {
    let lastWorkedTimeslot = "2024-05-22T11:58";
    const lambda = proxyquire.noCallThru().load("../app/eventHandler.js", {
      "./timeHelper.js": {
        actTime: () => {
          console.log(
            "CHIAMATA L'OVERRIDEEN!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
          );
          const dt = parseISO("2024-05-22T12:00");
          return dt;
        },
        parseISO,
        nextTimeSlot,
        isAfter,
      },

      "./dynamoFunctions.js": {
        getLastTimeSlotWorked: async (_unused1, _unused2) => lastWorkedTimeslot,
        setLastTimeSlotWorked: async (_unused1, _unused2, newTime) =>
          (lastWorkedTimeslot = newTime),
        getActionsByTimeSlot: async (
          _unused1,
          { timeSlot, _sub_unused1 },
          _unused2
        ) => {
          console.log("AAAAAAAA CALLED");
          switch (timeSlot) {
            case "2024-05-22T11:58":
              return require("./testData/11_58a.json");
            case "2024-05-22T11:59":
              return require("./testData/11_59.json");
            default:
              console.log("ERROR");
          }
        },
        batchDelete: async (_unused1, _unused2) => {},
      },
    });
    const result = await lambda.handleEvent();
    console.log("RESULT", result);
    expect(result).to.not.be.null;
    expect(result.statusCode).to.be.eq(200);
    expect(result).to.have.property("body");
    expect(result.body).to.be.eq(JSON.stringify({}));
  });
});
