const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire");

describe("processRecord.js", () => {
  let updateReworkStub, processRecord;

  beforeEach(() => {
    updateReworkStub = sinon.stub().resolves();
    processRecord = proxyquire("../app/processRecord.js", {
      "./putNotificationRework.js": { updateRework: updateReworkStub }
    }).processRecord;
  });

  afterEach(() => {
    sinon.restore();
  });

  it("NOTIFICATION_TIMELINE_REWORKED", async () => {
    const msg = { iun: "pk1", category: "NOTIFICATION_TIMELINE_REWORKED", reworkId: "sk1" };
    await processRecord(msg);
    expect(updateReworkStub.calledOnceWith(msg, "CREATED", "READY", true)).to.be.true;
  });

  it("SEND_ANALOG_PROGRESS", async () => {
    const msg = { iun: "pk2", category: "SEND_ANALOG_PROGRESS", reworkId: "sk2" };
    await processRecord(msg);
    expect(updateReworkStub.calledOnceWith(msg, "READY", "IN_PROGRESS", false)).to.be.true;
  });

  it("SEND_ANALOG_FEEDBACK", async () => {
    const msg = { iun: "pk3", category: "SEND_ANALOG_FEEDBACK", reworkId: "sk3" };
    await processRecord(msg);
    expect(updateReworkStub.calledOnceWith(msg, ["IN_PROGRESS", "READY"], "DONE", false)).to.be.true;
  });

  it("throw  error for category not handle", async () => {
    const msg = { iun: "pk4", category: "UNKNOWN", reworkId: "sk4" };
    try {
      await processRecord(msg);
      expect.fail("should have thrown");
    } catch (err) {
      expect(err.message).to.include("not managed");
    }
  });
});