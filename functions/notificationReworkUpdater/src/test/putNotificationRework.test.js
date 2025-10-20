const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire");

describe("putNotificationRework.js", () => {
  let updateWithConditionalStatusStub, putNotificationRework;

  beforeEach(() => {
    updateWithConditionalStatusStub = sinon.stub().resolves();
    putNotificationRework = proxyquire("../app/putNotificationRework.js", {
      "./dynamo": { updateWithConditionalStatus: updateWithConditionalStatusStub }
    });
  });

  afterEach(() => {
    sinon.restore();
  });

  it("call updateWithConditionalStatus without flagInvalidatedIds", async () => {
    const timelineElement = {
      iun: "pk1",
      reworkId: "sk1"
    };
    await putNotificationRework.updateRework(timelineElement, "CREATED", "IN_PROGRESS", false);

    expect(updateWithConditionalStatusStub.calledOnce).to.be.true;
    const args = updateWithConditionalStatusStub.firstCall.args;
    expect(args[0]).to.equal("pn-NotificationReworks");
    expect(args[1]).to.include({ iun: "pk1", reworkId: "sk1", status: "IN_PROGRESS" });
    expect(args[2]).to.equal("CREATED");
    expect(args[1]).to.have.property("updatedAt");
    expect(args[1]).to.not.have.property("flagInvalidatedIds");
  });

  it("call updateWithConditionalStatus with con flagInvalidatedIds", async () => {
    const timelineElement = {
      iun: "pk2",
      reworkId: "sk2",
      invalidatedTimelineElementIds: ["id1", "id2"]
    };
    await putNotificationRework.updateRework(timelineElement, "CREATED", "IN_PROGRESS", true);

    expect(updateWithConditionalStatusStub.calledOnce).to.be.true;
    const args = updateWithConditionalStatusStub.firstCall.args;
    expect(args[0]).to.equal("pn-NotificationReworks");
    expect(args[1]).to.have.property("iun", "pk2");
    expect(args[1]).to.have.property("reworkId", "sk2");
    expect(args[1]).to.have.property("status", "IN_PROGRESS");
    expect(args[1]).to.have.property("flagInvalidatedIds");
    expect(args[1].flagInvalidatedIds).to.deep.equal(["id1", "id2"]);
    expect(args[2]).to.equal("CREATED");
    expect(args[1]).to.have.property("updatedAt");
  });
});