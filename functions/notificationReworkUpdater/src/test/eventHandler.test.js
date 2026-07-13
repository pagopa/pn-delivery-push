const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire");

describe("eventHandler.js (SQS -> Lambda)", () => {
  let updateReworkStub;
  let updateRequestReworkStub;
  let getReworkEntityStub;
  let processRecordStub;
  let processUpdateRecordStub;
  let handler;

  beforeEach(() => {
    updateReworkStub = sinon.stub().resolves({ ok: true });
    updateRequestReworkStub = sinon.stub().resolves({ ok: true });
    getReworkEntityStub = sinon.stub().resolves({
      Item: {
        expectedStatusCodes: ["RECAG002C"],
        deliveryFailureCause: "M02"
      }
    });

    processRecordStub = sinon.stub().resolves({
      item: {
        iun: "iu-1",
        reworkId: "rw-1",
        status: "READY",
        category: "X"
      },
      expectedStates: ["CREATED"]
    });

    processUpdateRecordStub = sinon.stub().resolves({
      updateRequest: {
        expectedStatusCodes: ["RECAG001C"],
        expectedDeliveryFailureCause: "M01",
        status: "OK"
      },
      iun: "pkOK",
      reworkId: "skOK",
      status: "READY",
      expectedStatusCodes: ["RECAG001C"],
      deliveryFailureCause: "M01"
    });

    process.env.NOTIFICATION_REWORKS_DYNAMO_TABLENAME = "tableName";

    const mod = proxyquire("../app/eventHandler.js", {
      "./dynamo": {
        updateRework: updateReworkStub,
        updateRequestRework: updateRequestReworkStub,
        getReworkEntity: getReworkEntityStub
      },
      "./processRecord": {
        processRecord: processRecordStub,
        processUpdateRecord: processUpdateRecordStub
      }
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

  it("operation=ERROR valido -> chiama updateRework e nessun failure", async () => {
    const event = {
      Records: [
        {
          messageId: "m1",
          body: JSON.stringify({
            operation: "ERROR",
            iun: "pk1",
            reworkId: "sk1",
            error: ["e1", "e2"]
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnceWithExactly(
      {
        iun: "pk1",
        reworkId: "sk1",
        status: "ERROR",
        error: ["e1", "e2"]
      },
      null
    )).to.be.true;

    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("operation=ERROR con campi mancanti -> aggiunge failure", async () => {
    const event = {
      Records: [
        {
          messageId: "m2",
          body: JSON.stringify({
            operation: "ERROR",
            iun: "pk1",
            reworkId: null,
            error: "not-array"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.notCalled).to.be.true;
    expect(res).to.deep.equal({
      batchItemFailures: [{ itemIdentifier: "m2" }]
    });
  });

  it("operation=UPDATE -> chiama processRecord e poi updateRework", async () => {
    processRecordStub.resolves({
      item: {
        iun: "pkU",
        reworkId: "skU",
        status: "IN_PROGRESS",
        category: "C"
      },
      expectedStates: ["READY"]
    });

    const event = {
      Records: [
        {
          messageId: "m3",
          body: JSON.stringify({
            operation: "UPDATE",
            foo: "bar"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(processRecordStub.calledOnce).to.be.true;
    expect(updateReworkStub.calledOnceWithExactly(
      {
        iun: "pkU",
        reworkId: "skU",
        status: "IN_PROGRESS",
        category: "C"
      },
      ["READY"]
    )).to.be.true;

    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("operation=UPDATE con reworkRequestType=INVALIDATE_ELEMENTS -> forza status DONE", async () => {
    processRecordStub.resolves({
      item: {
        iun: "pkU",
        reworkId: "skU",
        status: "CREATED",
        category: "NOTIFICATION_TIMELINE_REWORKED"
      },
      expectedStates: ["CREATED"]
    });

    const event = {
      Records: [
        {
          messageId: "m4",
          body: JSON.stringify({
            operation: "UPDATE",
            iun: "pkU",
            reworkId: "skU",
            category: "NOTIFICATION_TIMELINE_REWORKED",
            reworkRequestType: "INVALIDATE_ELEMENTS"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(processRecordStub.calledOnce).to.be.true;
    expect(updateReworkStub.calledOnceWithExactly(
      {
        iun: "pkU",
        reworkId: "skU",
        status: "DONE",
        category: "NOTIFICATION_TIMELINE_REWORKED"
      },
      ["CREATED"]
    )).to.be.true;

    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("operation=UPDATE con category diversa non forza status DONE", async () => {
    processRecordStub.resolves({
      item: {
        iun: "pkU",
        reworkId: "skU",
        status: "CREATED",
        category: "OTHER_CATEGORY"
      },
      expectedStates: ["CREATED"]
    });

    const event = {
      Records: [
        {
          messageId: "m5",
          body: JSON.stringify({
            operation: "UPDATE",
            category: "OTHER_CATEGORY",
            reworkRequestType: "INVALIDATE_ELEMENTS"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnceWithExactly(
      {
        iun: "pkU",
        reworkId: "skU",
        status: "CREATED",
        category: "OTHER_CATEGORY"
      },
      ["CREATED"]
    )).to.be.true;

    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("operation=UPDATE con requestType diverso non forza status DONE", async () => {
    processRecordStub.resolves({
      item: {
        iun: "pkU",
        reworkId: "skU",
        status: "CREATED",
        category: "NOTIFICATION_TIMELINE_REWORKED"
      },
      expectedStates: ["CREATED"]
    });

    const event = {
      Records: [
        {
          messageId: "m6",
          body: JSON.stringify({
            operation: "UPDATE",
            category: "NOTIFICATION_TIMELINE_REWORKED",
            requestType: "OTHER_REQUEST_TYPE"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnceWithExactly(
      {
        iun: "pkU",
        reworkId: "skU",
        status: "CREATED",
        category: "NOTIFICATION_TIMELINE_REWORKED"
      },
      ["CREATED"]
    )).to.be.true;

    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("operation sconosciuta -> aggiunge failure", async () => {
    const event = {
      Records: [
        {
          messageId: "m7",
          body: JSON.stringify({ operation: "WHAT_IS_THIS" })
        }
      ]
    };

    const res = await handler(event);

    expect(processRecordStub.notCalled).to.be.true;
    expect(updateReworkStub.notCalled).to.be.true;
    expect(res).to.deep.equal({
      batchItemFailures: [{ itemIdentifier: "m7" }]
    });
  });

  it("JSON invalido -> aggiunge failure", async () => {
    const event = {
      Records: [
        {
          messageId: "m8",
          body: "{not-json"
        }
      ]
    };

    const res = await handler(event);

    expect(processRecordStub.notCalled).to.be.true;
    expect(updateReworkStub.notCalled).to.be.true;
    expect(res).to.deep.equal({
      batchItemFailures: [{ itemIdentifier: "m8" }]
    });
  });

  it("updateRework ritorna CONDITION_FAILED su UPDATE -> nessun failure", async () => {
    updateReworkStub.resolves({
      ok: false,
      reason: "CONDITION_FAILED"
    });

    const event = {
      Records: [
        {
          messageId: "m9",
          body: JSON.stringify({
            operation: "UPDATE",
            foo: "bar"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnce).to.be.true;
    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("updateRework ritorna CONDITION_FAILED su ERROR -> nessun failure", async () => {
    updateReworkStub.resolves({
      ok: false,
      reason: "CONDITION_FAILED"
    });

    const event = {
      Records: [
        {
          messageId: "m10",
          body: JSON.stringify({
            operation: "ERROR",
            iun: "pk10",
            reworkId: "sk10",
            error: ["err"]
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnce).to.be.true;
    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("errore fatale durante la gestione record -> aggiunge failure", async () => {
    updateReworkStub.rejects(new Error("boom"));

    const event = {
      Records: [
        {
          messageId: "m11",
          body: JSON.stringify({
            operation: "UPDATE",
            foo: "bar"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnce).to.be.true;
    expect(res).to.deep.equal({
      batchItemFailures: [{ itemIdentifier: "m11" }]
    });
  });

  it("gestisce più record con esiti misti", async () => {
    const event = {
      Records: [
        {
          messageId: "m12",
          body: JSON.stringify({
            operation: "ERROR",
            iun: "pk12",
            reworkId: "sk12",
            error: ["e12"]
          })
        },
        {
          messageId: "m13",
          body: "{"
        },
        {
          messageId: "m14",
          body: JSON.stringify({
            operation: "UNKNOWN"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(updateReworkStub.calledOnceWithExactly(
      {
        iun: "pk12",
        reworkId: "sk12",
        status: "ERROR",
        error: ["e12"]
      },
      null
    )).to.be.true;

    expect(res).to.deep.equal({
      batchItemFailures: [
        { itemIdentifier: "m13" },
        { itemIdentifier: "m14" }
      ]
    });
  });

  it("operation=UPDATE_REQUEST -> chiama processUpdateRecord, getReworkEntity e updateRequestRework", async () => {
    const item = {
      updateRequest: {
        expectedStatusCodes: ["RECAG001C"],
        expectedDeliveryFailureCause: "M01",
        status: "OK"
      },
      iun: "pkOK",
      reworkId: "skOK",
      status: "READY",
      expectedStatusCodes: ["RECAG001C"],
      deliveryFailureCause: "M01"
    };

    const reworkEntity = {
      Item: {
        expectedStatusCodes: ["RECAG002C"],
        deliveryFailureCause: "M02"
      }
    };

    processUpdateRecordStub.resolves(item);
    getReworkEntityStub.resolves(reworkEntity);

    const event = {
      Records: [
        {
          messageId: "m15",
          body: JSON.stringify({
            operation: "UPDATE_REQUEST",
            iun: "pkOK",
            reworkId: "skOK",
            updateValidationStatus: "OK",
            deliveryFailureCause: "M01",
            expectedStatusCodes: ["RECAG001C"]
          })
        }
      ]
    };

    const res = await handler(event);

    expect(processUpdateRecordStub.calledOnce).to.be.true;
    expect(processUpdateRecordStub.calledOnceWithExactly({
      operation: "UPDATE_REQUEST",
      iun: "pkOK",
      reworkId: "skOK",
      updateValidationStatus: "OK",
      deliveryFailureCause: "M01",
      expectedStatusCodes: ["RECAG001C"]
    })).to.be.true;

    expect(getReworkEntityStub.calledOnceWithExactly(item)).to.be.true;
    expect(updateRequestReworkStub.calledOnceWithExactly(item, reworkEntity)).to.be.true;

    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("operation=UPDATE_REQUEST con res ok=false -> nessun failure", async () => {
    updateRequestReworkStub.resolves({ ok: false });

    const event = {
      Records: [
        {
          messageId: "m16",
          body: JSON.stringify({
            operation: "UPDATE_REQUEST",
            iun: "pkKO",
            reworkId: "skKO",
            updateValidationStatus: "KO",
            error: ["err"]
          })
        }
      ]
    };

    const res = await handler(event);

    expect(processUpdateRecordStub.calledOnce).to.be.true;
    expect(getReworkEntityStub.calledOnce).to.be.true;
    expect(updateRequestReworkStub.calledOnce).to.be.true;

    expect(res).to.deep.equal({ batchItemFailures: [] });
  });

  it("operation=UPDATE_REQUEST con errore fatale -> aggiunge failure", async () => {
    processUpdateRecordStub.rejects(new Error("boom update request"));

    const event = {
      Records: [
        {
          messageId: "m17",
          body: JSON.stringify({
            operation: "UPDATE_REQUEST",
            iun: "pkERR",
            reworkId: "skERR"
          })
        }
      ]
    };

    const res = await handler(event);

    expect(processUpdateRecordStub.calledOnce).to.be.true;
    expect(getReworkEntityStub.notCalled).to.be.true;
    expect(updateRequestReworkStub.notCalled).to.be.true;

    expect(res).to.deep.equal({
      batchItemFailures: [{ itemIdentifier: "m17" }]
    });
  });
});