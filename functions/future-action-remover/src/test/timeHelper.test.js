const { expect, assert } = require("chai");
const { describe, it } = require("mocha");

const {
  isAfter,
  parseISO,
  actTime,
  nextTimeSlot,
  toString,
  isAfterEq,
  isEqStr,
  isAfterEqStr,
  isAfterStr,
  convertFromEpochToIsoDateTime
} = require("../app/timeHelper");

const { InvalidDateException } = require("../app/exceptions");

const futureDate = () => {
    const date = new Date();
    date.setDate(date.getDate() + 2);
    return date.toISOString();
}

const futureDateStr = futureDate().toString();

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

  it("isAfter: false", () => {
      const dt = parseISO(futureDate());
      const act = actTime();

      const res = isAfter(act, dt);

      expect(res).to.be.false;
  });

  it("isAfter: true", () => {
    const dt = parseISO(futureDate());
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
    const dt = parseISO(futureDate());
    const act = actTime();

    const res = isAfterEq(dt, act);

    expect(res).to.be.true;
  });

  it("isAfterEqStr: major => true", () => {
    const act = actTime().toString()
    const res = isAfterEqStr(futureDateStr, act);

    expect(res).to.be.true;
  });

  it("isAfterStr: major => true", () => {
    const act = actTime().toString()
    const res = isAfterStr(futureDateStr, act);

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

  it("isAfterEqStr: equal => true", () => {
    const dateStr1 = "2024-12-01T12:01:00Z";
    const dateStr2 = "2024-12-01T12:01:00Z";
    const res2 = isAfterEqStr(dateStr2, dateStr1);
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

  it("same time in string", () => {
    const dateStr = "2024-12-01T12:01:00Z";
    const dateStr1 = "2024-12-01T12:01:01Z";
    const res = isEqStr(dateStr, dateStr1);
    expect(res).to.be.equal(true);
  });

  it("different time in string", () => {
    const dateStr = "2024-12-01T12:01:00Z";
    const dateStr1 = "2024-12-01T12:02:00Z";
    const res = isEqStr(dateStr, dateStr1);
    expect(res).to.be.equal(false);
  });

  it("convertFromEpochToIsoDateTime: valid epoch number", () => {
    const epoch = 1752066000; // Example epoch time
    const expectedDateTime = "2025-07-09T13:00:00.000Z"; // Expected ISO string
    const converted = convertFromEpochToIsoDateTime(epoch);
    expect(converted).to.equal(expectedDateTime);
  });

  it("convertFromEpochToIsoDateTime: valid epoch string", () => {
    const epoch = "1707742800"; // Example epoch time
    const expectedDateTime = "2024-02-12T13:00:00.000Z"; // Expected ISO string
    const converted = convertFromEpochToIsoDateTime(epoch);
    expect(converted).to.equal(expectedDateTime);
  });

  it("convertFromEpochToIsoDateTime: invalid epoch", () => {
    const epoch = "invalid-epoch"; // Invalid epoch time
    expect(() => convertFromEpochToIsoDateTime(epoch)).to.throw(InvalidDateException);
  });
});
