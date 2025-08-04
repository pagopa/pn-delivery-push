const { expect } = require("chai");
const { nDaysFromNowAsUNIXTimestamp } = require("../app/lib/utils.js");
const sinon = require("sinon");
const { isPersistenceEnabled } = require("../app/lib/utils.js");

// test utils
describe("utils tests", function () {
  it("test nDaysFromNowAsUNIXTimestamp", async () => {
    const res = nDaysFromNowAsUNIXTimestamp(1);
    expect(res).equal(Math.floor(Date.now() / 1000) + 86400);
  });

  it("test nDaysFromNowAsUNIXTimestamp 0", async () => {
    const res = nDaysFromNowAsUNIXTimestamp(0);
    expect(res).equal(0);
  });

  it("test nDaysFromNowAsUNIXTimestamp -1", async () => {
    const res = nDaysFromNowAsUNIXTimestamp(-1);
    expect(res).equal(0);
  });
});

describe("isPersistenceEnabled", function () {
  let originalEnv;
  let dateNowStub;
  let consoleWarnStub;
  let consoleInfoStub;

  beforeEach(() => {
    originalEnv = { ...process.env };
    dateNowStub = sinon.stub(Date, "now");
    consoleWarnStub = sinon.stub(console, "warn");
    consoleInfoStub = sinon.stub(console, "info");
  });

  afterEach(() => {
    process.env = originalEnv;
    dateNowStub.restore();
    consoleWarnStub.restore();
    consoleInfoStub.restore();
  });

  it("returns false and warns if env vars are missing", () => {
    delete process.env.ACTION_LAMBDA_ENABLED_START;
    delete process.env.ACTION_LAMBDA_ENABLED_END;
    expect(isPersistenceEnabled()).to.be.false;
    expect(consoleWarnStub.calledOnce).to.be.true;
    expect(consoleWarnStub.firstCall.args[0]).to.include("not set");
  });

  it("returns false and warns if only start is missing", () => {
    delete process.env.ACTION_LAMBDA_ENABLED_START;
    process.env.ACTION_LAMBDA_ENABLED_END = "9999999999999";
    expect(isPersistenceEnabled()).to.be.false;
    expect(consoleWarnStub.calledOnce).to.be.true;
  });

  it("returns false and warns if only end is missing", () => {
    process.env.ACTION_LAMBDA_ENABLED_START = "1000";
    delete process.env.ACTION_LAMBDA_ENABLED_END;
    expect(isPersistenceEnabled()).to.be.false;
    expect(consoleWarnStub.calledOnce).to.be.true;
  });

  it("returns false and infos if current time is before start", () => {
    process.env.ACTION_LAMBDA_ENABLED_START = "2000";
    process.env.ACTION_LAMBDA_ENABLED_END = "3000";
    dateNowStub.returns(1500);
    expect(isPersistenceEnabled()).to.be.false;
    expect(consoleInfoStub.calledOnce).to.be.true;
    expect(consoleInfoStub.firstCall.args[0]).to.include("outside of the range");
  });

  it("returns false and infos if current time is after end", () => {
    process.env.ACTION_LAMBDA_ENABLED_START = "2000";
    process.env.ACTION_LAMBDA_ENABLED_END = "3000";
    dateNowStub.returns(3500);
    expect(isPersistenceEnabled()).to.be.false;
    expect(consoleInfoStub.calledOnce).to.be.true;
    expect(consoleInfoStub.firstCall.args[0]).to.include("outside of the range");
  });

  it("returns true and infos if current time is within range", () => {
    process.env.ACTION_LAMBDA_ENABLED_START = "2000";
    process.env.ACTION_LAMBDA_ENABLED_END = "3000";
    dateNowStub.returns(2500);
    expect(isPersistenceEnabled()).to.be.true;
    expect(consoleInfoStub.calledOnce).to.be.true;
    expect(consoleInfoStub.firstCall.args[0]).to.include("within the range");
  });
});
