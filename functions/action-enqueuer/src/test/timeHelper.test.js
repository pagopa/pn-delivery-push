const { expect, assert, should } = require("chai");
const sinon = require("sinon");

const {
  nextTimeSlot,
  isAfter,
  parseISO,
  actTime,
  toString,
} = require("../app/timeHelper");

const { InvalidDateException } = require("../app/exceptions");

//const { mockIamPolicy } = require("./mocks");

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

  /*
  it("anonymize with length < 6", () => {
    const text = "test";
    const anonymized = anonymizeKey(text);
    expect(anonymized).equals("****");
  });

  it("anonymize undefined string", () => {
    const text = undefined;
    const anonymized = anonymizeKey(text);
    expect(anonymized).equals("");
  });
});

describe("Test logEvent", () => {
  it("", () => {
    const spy = sinon.spy(console, "info");
    const mockedEvent = {
      path: "/request",
      httpMethod: "GET",
      headers: {
        "x-api-key": "datatohide",
        "X-Amzn-Trace-Id": "test",
      },
    };
    logEvent(mockedEvent);
    const expectedEvent = {
      httpMethod: "GET",
      path: "/request",
      "X-Amzn-Trace-Id": "test",
      "x-api-key": "da******de",
    };
    expect(
      spy
        .getCall(0)
        .calledWith("New event received", sinon.match(expectedEvent))
    ).to.be.true;
    spy.restore();
  });
});

describe("Test logIamPolicy", () => {
  it("", () => {
    const spy = sinon.spy(console, "log");
    logIamPolicy(mockIamPolicy);
    const expectedIamPolicy = {
      principalId: "testPrincipal",
      policyDocument: {
        Version: "2012-10-17",
        Statement: [
          {
            Action: "execute-api:Invoke",
            Effect: "Allow",
            Resource: "arn",
          },
        ],
      },
      context: {
        uid: "APIKEY-te******ey",
        cx_id: "cxId",
        cx_groups: "group1,group2",
        cx_type: "PA",
      },
      usageIdentifierKey: "te******ey",
    };
    expect(
      spy.getCall(0).calledWith("IAM Policy:", sinon.match(expectedIamPolicy))
    ).to.be.true;
    spy.restore();
  });
});

describe("Test findAttributeValueInObjectWithInsensitiveCase", () => {
  it("item found", () => {
    const object = {
      key: "test",
    };
    const value = findAttributeValueInObjectWithInsensitiveCase(object, "KEY");
    expect(value).equals("test");
  });

  it("item not found", () => {
    const object = {
      key: "test",
    };
    const value = findAttributeValueInObjectWithInsensitiveCase(object, "foo");
    expect(value).to.be.undefined;
  });
*/
});
