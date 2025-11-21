const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire");

describe("eventHandler.js (SQS → Lambda)", () => {
  let updateReworkStub, processRecordStub, handler;

  beforeEach(() => {
    updateReworkStub = sinon.stub().resolves({ ok: true });
    processRecordStub = sinon.stub().resolves({
      item: { iun: "iu-1", reworkId: "rw-1", status: "READY", category: "X" },
      expectedStates: ["CREATED"]
    });
    process.env.NOTIFICATION_REWORKS_DYNAMO_TABLENAME='tableName';

    const mod = proxyquire("../app/eventHandler.js", {
      "./dynamo": { updateRework: updateReworkStub },
      "./processRecord": { processRecord: processRecordStub }
    });

    handler = mod.handleEvent;
  });

  afterEach(() => {
    sinon.restore();
  });

  it("ritorna batchItemFailures vuoto se non ci sono records", async () => {
    const res = await handler({});
    expect(res).to.deep.equal({ batchItemFailures: [] });
    expect(updateReworkStub.notCalled).to.be.true;
    expect(processRecordStub.notCalled).to.be.true;
  });

  it("operationType=ERROR valido → chiama updateRework e nessun failure", async () => {
    const event = {
      Records: [
        {
          messageId: "m1",
          body: JSON.stringify({
            operationType: "ERROR",
            iun: "pk1",
            reworkId: "sk1",
            errors: ["e1", "e2"]
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnceWithExactly(
      { iun: "pk1", reworkId: "sk1", status: "ERROR", errors: ["e1", "e2"] },
      null
    )).to.be.true;
    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("operationType=ERROR con campi mancanti → aggiunge failure", async () => {
    const event = {
      Records: [
        {
          messageId: "m2",
          body: JSON.stringify({
            operationType: "ERROR",
            iun: "pk1",
            reworkId: null,
            errors: "not-array"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.notCalled).to.be.true;
    expect(res).to.deep.equal({ batchItemFailures: [{ itemIdentifier: "m2" }] });
  });

  it("operationType=UPDATE → chiama processRecord e poi updateRework", async () => {
    processRecordStub.resolves({
      item: { iun: "pkU", reworkId: "skU", status: "IN_PROGRESS", category: "C" },
      expectedStates: ["READY"]
    });

    const event = {
      Records: [
        {
          messageId: "m3",
          body: JSON.stringify({
            operationType: "UPDATE",
            foo: "bar"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(processRecordStub.calledOnce).to.be.true;
    expect(updateReworkStub.calledOnceWithExactly(
      { iun: "pkU", reworkId: "skU", status: "IN_PROGRESS", category: "C" },
      ["READY"]
    )).to.be.true;
    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("operationType sconosciuto → aggiunge failure", async () => {
    const event = {
      Records: [
        {
          messageId: "m4",
          body: JSON.stringify({ operationType: "WHAT_IS_THIS" })
        }
      ]
    };

    const res = await handler(event);

    expect(processRecordStub.notCalled).to.be.true;
    expect(updateReworkStub.notCalled).to.be.true;
    expect(res).to.deep.equal({ batchItemFailures: [{ itemIdentifier: "m4" }] });
  });

  it("JSON invalido → aggiunge failure", async () => {
    const event = {
      Records: [
        {
          messageId: "m5",
          body: "{not-json"
        }
      ]
    };

    const res = await handler(event);
    expect(res).to.deep.equal({ batchItemFailures: [{ itemIdentifier: "m5" }] });
    expect(processRecordStub.notCalled).to.be.true;
    expect(updateReworkStub.notCalled).to.be.true;
  });

  it("updateRework ritorna CONDITION_FAILED → nessun failure (no retry)", async () => {
    updateReworkStub.resolves({ ok: false, reason: "CONDITION_FAILED" });

    const event = {
      Records: [
        {
          messageId: "m6",
          body: JSON.stringify({
            operationType: "UPDATE",
            foo: "bar"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnce).to.be.true;
    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("errore fatale durante la gestione record → aggiunge failure", async () => {
    updateReworkStub.rejects(new Error("boom"));

    const event = {
      Records: [
        {
          messageId: "m7",
          body: JSON.stringify({
            operationType: "UPDATE",
            foo: "bar"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnce).to.be.true;
    expect(res).to.deep.equal({ batchItemFailures: [{ itemIdentifier: "m7" }] });
  });

  it("gestisce più record con esiti misti", async () => {
    // Prepariamo: primo OK (ERROR valido), secondo JSON invalido, terzo op sconosciuta
    const event = {
      Records: [
        {
          messageId: "m8",
          body: JSON.stringify({
            operationType: "ERROR",
            iun: "pk8",
            reworkId: "sk8",
            errors: ["e8"]
          })
        },
        { messageId: "m9", body: "{" }, // invalid JSON
        {
          messageId: "m10",
          body: JSON.stringify({ operationType: "UNKNOWN" })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnceWithExactly(
      { iun: "pk8", reworkId: "sk8", status: "ERROR", errors: ["e8"] },
      null
    )).to.be.true;

    expect(res).to.deep.equal({
      batchItemFailures: [
        { itemIdentifier: "m9" },
        { itemIdentifier: "m10" }
      ]
    });
  });
});
