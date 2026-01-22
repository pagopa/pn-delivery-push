const { expect } = require("chai");

describe("processRecord.js", () => {
  let processRecord;

  beforeEach(() => {
    ({ processRecord, processUpdateRecord } = require("../app/processRecord.js"));
  });

  it("NOTIFICATION_TIMELINE_REWORKED → CREATED -> READY", async () => {
    const msg = { iun: "pk1", category: "NOTIFICATION_TIMELINE_REWORKED", reworkId: "sk1" };
    const { item, expectedStates } = await processRecord(msg);

    expect(expectedStates).to.deep.equal(["CREATED"]);
    expect(item).to.deep.equal({
      iun: "pk1",
      reworkId: "sk1",
      status: "READY"
    });
  });

  it("SEND_ANALOG_PROGRESS → READY -> IN_PROGRESS", async () => {
    const msg = { iun: "pk2", category: "SEND_ANALOG_PROGRESS", reworkId: "sk2" };
    const { item, expectedStates } = await processRecord(msg);

    expect(expectedStates).to.deep.equal(["READY"]);
    expect(item.status).to.equal("IN_PROGRESS");
  });

  it("SEND_ANALOG_FEEDBACK → READY|IN_PROGRESS -> DONE", async () => {
    const msg = { iun: "pk4", category: "SEND_ANALOG_FEEDBACK", reworkId: "sk4" };
    const { item, expectedStates } = await processRecord(msg);

    expect(expectedStates).to.deep.equal(["READY", "IN_PROGRESS"]);
    expect(item.status).to.equal("DONE");
  });

  it("propaga i campi opzionali (timelineElementIds, error) se presenti", async () => {
    const msg = {
      iun: "pk6",
      reworkId: "sk6",
      category: "SEND_ANALOG_PROGRESS",
      timelineElementIds: ["t1", "t2"],
      error: ["e1"]
    };
    const { item } = await processRecord(msg);
    expect(item.error).to.deep.equal(["e1"]);
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

  it("OK: genera item con status READY e campi attesi", async () => {
    const msg = {
      iun: "pk1",
      reworkId: "sk1",
      updateValidationStatus: "OK",
      expectedStatusCodes: ["A"],
      expectedDeliveryFailureCause: "cause"
    };
    const item = await processUpdateRecord(msg);
    expect(item.iun).to.equal("pk1");
    expect(item.reworkId).to.equal("sk1");
    expect(item.status).to.equal("READY");
    expect(item.updateRequest.status).to.equal("OK");
    expect(item.deliveryFailureCause).to.equal("cause");
    expect(item.expectedStatusCodes).to.deep.equal(["A"]);
  });

  it("KO: genera item con error", async () => {
    const msg = {
      iun: "pk2",
      reworkId: "sk2",
      updateValidationStatus: "KO",
      error: ["err"]
    };
    const item = await processUpdateRecord(msg);
    expect(item.status).to.equal("READY");
    expect(item.updateRequest.status).to.equal("KO");
    expect(item.updateRequest.error).to.deep.equal(["err"]);
  });

  it("lancia errore se mancano iun o reworkId", async () => {
    try {
      await processUpdateRecord({ updateValidationStatus: "OK" });
      expect.fail("Doveva lanciare");
    } catch (err) {
      expect(err.message).to.match(/Missing required fields/);
    }
  });
});
