const { expect, assert } = require("chai");
const { describe, it } = require("mocha");

const {
  isAfter,
  parseISO,
  actTime,
  nextTimeSlot,
  toString,
  isAfterEq,
} = require("../app/timeHelper");

const { InvalidDateException } = require("../app/exceptions");

describe("Test Time Functions 1", () => {
  it("parse right", () => {
    const dateStr = "2024-10-23T12:00:00Z";
    const dt = parseISO(dateStr);

    expect(dt.isValid).equals(true);
  });

  it("parse wrong", () => {
    const dateStr = "a024-10-23T12:00:00Z";
    //.should.throw(new InvalidateException(dateStr));
    assert.throws(
      () => {
        parseISO(dateStr);
      },
      InvalidDateException //,
      // "Unable to generate policy statement"
    );
  });

  it.only("isAfter: false", () => {
    const dateStr = "2024-12-01T12:01:00Z";
    const dt = parseISO(dateStr);
    const act = actTime();

    const res = isAfter(act, dt);

    expect(res).to.be.false;
  });

  it("isAfter: true", () => {
    const dateStr = "2024-12-01T12:01:00Z";
    const dt = parseISO(dateStr);
    const act = actTime();

    const res = isAfter(dt, act);

    expect(res).to.be.true;
  });

  it("isAfter: wrong format 1", () => {
    const dateStr = "a2023-10-23T12:00:00Z";

    const act = actTime();

    expect(() => isAfter(dateStr, act)).to.throw(InvalidDateException);
  });
  it("isAfter: wrong format 2", () => {
    const dateStr = "2a023-10-23T12:00:00Z";

    const act = actTime();

    expect(() => isAfter(act, dateStr)).to.throw(InvalidDateException);
  });

  it("isAfter: wrong format 1", () => {
    const dateStr = "a2023-10-23T12:00:00Z";

    const act = actTime();

    expect(() => isAfter(dateStr, act)).to.throw(InvalidDateException);
  });

  it("isAfterEq: major => true", () => {
    const dateStr = "2024-12-01T12:01:00Z";
    const dt = parseISO(dateStr);
    const act = actTime();

    const res = isAfterEq(dt, act);

    expect(res).to.be.true;
  });

  it("isAfterEq: equal => true", () => {
    const dateStr1 = "2024-12-01T12:01:00Z";
    const dt1 = parseISO(dateStr1);
    const dateStr2 = "2024-12-01T12:01:00Z";
    const dt2 = parseISO(dateStr2);

    const res1 = isAfterEq(dt1, dt2);
    expect(res1).to.be.true;

    const res2 = isAfterEq(dt2, dt1);
    expect(res2).to.be.true;
  });

  it("nextTimeSlot: during day", () => {
    const dateStr = "2023-10-23T12:00:00Z";
    const dt = parseISO(dateStr);
    const res = toString(nextTimeSlot(dt));

    expect(res).to.be.equal("2023-10-23T12:01");
  });

  it("nextTimeSlot: midnight", () => {
    const dateStr = "2023-10-23T23:59:00Z";
    const dt = parseISO(dateStr);
    const res = toString(nextTimeSlot(dt));

    expect(res).to.be.equal("2023-10-24T00:00");
  });
});
