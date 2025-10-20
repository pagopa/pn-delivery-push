const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire");

describe("eventHandler.js", () => {
  let updateStatusAndErrorsStub, processRecordStub, handleEvent;
  const TABLE_NAME = "test-table";

  beforeEach(() => {
    process.env.NOTIFICATION_REWORKS_DYNAMO_TABLENAME = TABLE_NAME;
    updateStatusAndErrorsStub = sinon.stub().resolves();
    processRecordStub = sinon.stub().resolves();

    handleEvent = proxyquire("../app/eventHandler.js", {
      "./dynamo": { updateStatusAndErrors: updateStatusAndErrorsStub },
      "./processRecord": { processRecord: processRecordStub }
    }).handleEvent;
  });

  afterEach(() => {
    sinon.restore();
    delete process.env.NOTIFICATION_REWORKS_DYNAMO_TABLENAME;
  });

  it("Returns message if there are no records", async () => {
    const res = await handleEvent({});
    expect(res).to.deep.equal({ message: "no items found to be updated" });
    expect(updateStatusAndErrorsStub.notCalled).to.be.true;
    expect(processRecordStub.notCalled).to.be.true;
  });

  it("call updateStatusAndErrors for operationType ERROR", async () => {
    const event = {
      Records: [
        {
          body: JSON.stringify({
            operationType: "ERROR",
            iun: "pk1",
            reworkId: "sk1",
            errors: ["err1", "err2"]
          })
        }
      ]
    };
    const res = await handleEvent(event);
    expect(updateStatusAndErrorsStub.calledOnceWith(
      TABLE_NAME, "pk1", "sk1", ["err1", "err2"]
    )).to.be.true;
    expect(res).to.deep.equal({ message: "items updated" });
  });

  it("Don't call updateStatusAndErrors if ERROR fields are missing", async () => {
    const event = {
      Records: [
        {
          body: JSON.stringify({
            operationType: "ERROR",
            iun: "pk1",
            reworkId: null,
            errors: "not-an-array"
          })
        }
      ]
    };
    const res = await handleEvent(event);
    expect(updateStatusAndErrorsStub.notCalled).to.be.true;
    expect(res).to.deep.equal({ message: "no items found to be updated" });
  });

  it("call processRecord for operationType UPDATE", async () => {
    const event = {
      Records: [
        {
          body: JSON.stringify({
            operationType: "UPDATE",
            foo: "bar"
          })
        }
      ]
    };
    const res = await handleEvent(event);
    expect(processRecordStub.calledOnce).to.be.true;
    expect(res).to.deep.equal({ message: "items updated" });
  });

  it("manages multiple records with different types", async () => {
    const event = {
      Records: [
        {
          body: JSON.stringify({
            operationType: "ERROR",
            iun: "pk1",
            reworkId: "sk1",
            errors: ["err1"]
          })
        },
        {
          body: JSON.stringify({
            operationType: "UPDATE",
            foo: "bar"
          })
        }
      ]
    };
    const res = await handleEvent(event);
    expect(updateStatusAndErrorsStub.calledOnce).to.be.true;
    expect(processRecordStub.calledOnce).to.be.true;
    expect(res).to.deep.equal({ message: "items updated" });
  });

  it("Returns message if no valid record", async () => {
    const event = {
      Records: [
        {
          body: JSON.stringify({
            operationType: "ERROR",
            iun: null,
            reworkId: null,
            errors: null
          })
        }
      ]
    };
    const res = await handleEvent(event);
    expect(updateStatusAndErrorsStub.notCalled).to.be.true;
    expect(processRecordStub.notCalled).to.be.true;
    expect(res).to.deep.equal({ message: "no items found to be updated" });
  });
});