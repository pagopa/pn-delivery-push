const { insideWorkingWindow } = require("../app/workingTimeUtils");

const { expect } = require("chai");
const { describe, it } = require("mocha");

describe("test insideWorkingWindow", () => {
  it("isIn", () => {
    const fakeAction = {
      notBefore: "2024-05-12T13:00:00.384Z",
      iun: "iunFake",
      timeSlot: "2024-05-12T13:00:00",
      actionId: "actionIdFake",
      createdAt: "2024-05-12T13:30:00.384Z"
    };

    const result = insideWorkingWindow(
      fakeAction,
      "2024-05-12T12:00:00",
      "2024-05-12T14:00:00"
    );
    expect(result).to.be.true;
  });
  it("isOut", () => {
    const fakeAction = {
      notBefore: "2024-05-12T14:00:00.384Z",
      iun: "iunFake",
      timeSlot: "2024-05-12T13:00:00",
      actionId: "actionIdFake",
      createdAt: "2024-05-12T15:00:00.384Z"
    };

    const result = insideWorkingWindow(
      fakeAction,
      "2024-05-12T12:00:00",
      "2024-05-12T14:00:00"
    );
    expect(result).to.be.false;
  });
  it("Action without createdAt", () => {
    const fakeAction = {
      iun: "iunFake",
      timeSlot: "2024-05-12T13:00:00",
      actionId: "actionIdFake",
    };

    const result = insideWorkingWindow(
      fakeAction,
      "2024-05-12T12:00:00",
      "2024-05-12T14:00:00"
    );
    // If createdAt is not present, we assume the action is inside the working window
    expect(result).to.be.true;
  });
});
