const { isLambdaDisabled } = require("../app/utils");
const sinon = require("sinon");
const chai = require("chai");
const expect = chai.expect;

describe("isLambdaDisabled", function() {
  let clock;

  afterEach(() => {
    if (clock) clock.restore();
  });

  it("should return true if currentDate is before start", () => {
    // Set current date to 2023-01-01T00:00:00.000Z
    clock = sinon.useFakeTimers(new Date("2023-01-01T00:00:00.000Z").getTime());
    const featureFlag = {
      start: "2023-02-01T00:00:00.000Z",
      end: "2023-03-01T00:00:00.000Z"
    };
    expect(isLambdaDisabled(featureFlag)).to.be.true;
  });

  it("should return true if currentDate is after end", () => {
    // Set current date to 2023-04-01T00:00:00.000Z
    clock = sinon.useFakeTimers(new Date("2023-04-01T00:00:00.000Z").getTime());
    const featureFlag = {
      start: "2023-02-01T00:00:00.000Z",
      end: "2023-03-01T00:00:00.000Z"
    };
    expect(isLambdaDisabled(featureFlag)).to.be.true;
  });

  it("should return false if currentDate is between start and end", () => {
    // Set current date to 2023-02-15T12:00:00.000Z
    clock = sinon.useFakeTimers(new Date("2023-02-15T12:00:00.000Z").getTime());
    const featureFlag = {
      start: "2023-02-01T00:00:00.000Z",
      end: "2023-03-01T00:00:00.000Z"
    };
    expect(isLambdaDisabled(featureFlag)).to.be.false;
  });
});