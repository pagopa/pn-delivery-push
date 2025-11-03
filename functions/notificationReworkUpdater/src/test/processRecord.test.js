const { expect } = require("chai");

describe("processRecord.js", () => {
  let processRecord;

  beforeEach(() => {
    ({ processRecord } = require("../app/processRecord.js"));
  });

  it("NOTIFICATION_TIMELINE_REWORKED → CREATED -> READY", async () => {
    const msg = { iun: "pk1", category: "NOTIFICATION_TIMELINE_REWORKED", reworkId: "sk1" };
    const { item, expectedStates } = await processRecord(msg);

    expect(expectedStates).to.deep.equal(["CREATED"]);
    expect(item).to.deep.equal({
      iun: "pk1",
      reworkId: "sk1",
      category: "NOTIFICATION_TIMELINE_REWORKED",
      status: "READY"
    });
  });

  it("SEND_ANALOG_PROGRESS → READY -> IN_PROGRESS", async () => {
    const msg = { iun: "pk2", category: "SEND_ANALOG_PROGRESS", reworkId: "sk2" };
    const { item, expectedStates } = await processRecord(msg);

    expect(expectedStates).to.deep.equal(["READY"]);
    expect(item.status).to.equal("IN_PROGRESS");
    expect(item.category).to.equal("SEND_ANALOG_PROGRESS");
  });

  it("SEND_ANALOG_FEEDBACK → READY|IN_PROGRESS -> DONE", async () => {
    const msg = { iun: "pk4", category: "SEND_ANALOG_FEEDBACK", reworkId: "sk4" };
    const { item, expectedStates } = await processRecord(msg);

    expect(expectedStates).to.deep.equal(["READY", "IN_PROGRESS"]);
    expect(item.status).to.equal("DONE");
    expect(item.category).to.equal("SEND_ANALOG_FEEDBACK");
  });

  it("propaga i campi opzionali (timelineElementIds, errors) se presenti", async () => {
    const msg = {
      iun: "pk6",
      reworkId: "sk6",
      category: "SEND_ANALOG_PROGRESS",
      timelineElementIds: ["t1", "t2"],
      errors: ["e1"]
    };
    const { item } = await processRecord(msg);

    expect(item.timelineElementIds).to.deep.equal(["t1", "t2"]);
    expect(item.errors).to.deep.equal(["e1"]);
  });

  it("lancia se mancano i campi obbligatori", async () => {
    const msg = { iun: "pk7", category: "SEND_ANALOG_PROGRESS" };
    try {
      await processRecord(msg);
      expect.fail("doveva lanciare");
    } catch (err) {
      expect(err.message).to.include("Missing required fields");
    }
  });

  it("lancia su categoria sconosciuta", async () => {
    const msg = { iun: "pk8", category: "UNKNOWN", reworkId: "sk8" };
    try {
      await processRecord(msg);
      expect.fail("doveva lanciare");
    } catch (err) {
      expect(err.message).to.include("Unknown category");
    }
  });
});
