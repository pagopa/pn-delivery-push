// test/dynamo.spec.js
const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire");

describe("updateRework (dynamo.js)", () => {
  let sendStub;
  let UpdateCommandCtorStub;
  let DynamoDBDocumentClientFromStub;
  let restoreClock;
  let dynamo;

  const TABLE_NAME = "TestTable";
  const FIXED_NOW = new Date("2024-06-01T12:00:00Z");

  beforeEach(() => {
    restoreClock = sinon.useFakeTimers({ now: FIXED_NOW, shouldAdvanceTime: false });

    process.env.NOTIFICATION_REWORKS_DYNAMO_TABLENAME = TABLE_NAME;
    process.env.AWS_REGION = "eu-central-1";

    sendStub = sinon.stub().resolves();
    DynamoDBDocumentClientFromStub = sinon.stub().returns({ send: sendStub });

    UpdateCommandCtorStub = sinon.stub().callsFake((params) => ({ __params: params }));

    dynamo = proxyquire("../app/dynamo.js", {
      "@aws-sdk/client-dynamodb": {
        DynamoDBClient: function DynamoDBClient() {}
      },
      "@aws-sdk/lib-dynamodb": {
        DynamoDBDocumentClient: { from: DynamoDBDocumentClientFromStub },
        UpdateCommand: UpdateCommandCtorStub
      }
    });
  });

  afterEach(() => {
    sinon.restore();
    restoreClock && restoreClock.restore();
    delete process.env.NOTIFICATION_REWORKS_DYNAMO_TABLENAME;
    delete process.env.AWS_REGION;
  });

  it("invia UpdateCommand con ConditionExpression quando expectedStates è presente (non-ERROR)", async () => {
    const item = {
      iun: "pk1",
      reworkId: "sk1",
      status: "IN_PROGRESS",
      category: "SEND_ANALOG_PROGRESS",
      timelineElementIds: ["t1", "t2"]
    };

    const res = await dynamo.updateRework(item, ["CREATED", "READY"]);

    expect(res).to.deep.equal({ ok: true });
    expect(UpdateCommandCtorStub.calledOnce).to.be.true;

    const sent = UpdateCommandCtorStub.firstCall.args[0];
    expect(sent.TableName).to.equal(TABLE_NAME);
    expect(sent.Key).to.deep.equal({ iun: "pk1", reworkId: "sk1" });
    expect(sent.UpdateExpression).to.include("SET #s = :newStatus");
    expect(sent.UpdateExpression).to.include("#updatedAt = :updatedAt");

    expect(sent.UpdateExpression).to.include("#category = :category");
    expect(sent.UpdateExpression).to.include("#timelineElementIds = :timelineElementIds");

    expect(sent.ConditionExpression).to.include("#s IN");
    expect(sent.ExpressionAttributeValues[":state0"]).to.equal("CREATED");
    expect(sent.ExpressionAttributeValues[":state1"]).to.equal("READY");

    expect(sent.ExpressionAttributeValues[":newStatus"]).to.equal("IN_PROGRESS");
    expect(sent.ExpressionAttributeValues[":updatedAt"]).to.equal(FIXED_NOW.toISOString());

    expect(sendStub.calledOnce).to.be.true;
  });

  it("non mette ConditionExpression quando status = ERROR (ignora expectedStates)", async () => {
    const item = {
      iun: "pk2",
      reworkId: "sk2",
      status: "ERROR",
      error: ["e1", "e2"]
    };

    const res = await dynamo.updateRework(item, ["CREATED"]);
    expect(res).to.deep.equal({ ok: true });

    const sent = UpdateCommandCtorStub.firstCall.args[0];
    expect(sent.ConditionExpression).to.be.undefined;
    expect(sent.UpdateExpression).to.include("#s = :newStatus");
    expect(sent.UpdateExpression).to.include("#error = :error");
    expect(sent.ExpressionAttributeValues[":newStatus"]).to.equal("ERROR");
    expect(sent.ExpressionAttributeValues[":error"]).to.deep.equal(["e1", "e2"]);
  });

  it("ritorna { ok:false, reason:'CONDITION_FAILED' } su ConditionalCheckFailedException", async () => {
    sendStub.rejects({ name: "ConditionalCheckFailedException" });

    const item = { iun: "pk3", reworkId: "sk3", status: "READY" };
    const res = await dynamo.updateRework(item, ["CREATED"]);

    expect(res).to.deep.equal({ ok: false, reason: "CONDITION_FAILED" });
    expect(sendStub.calledOnce).to.be.true;
  });

  it("rilancia errori generici", async () => {
    sendStub.rejects({ name: "OtherError", message: "boom" });

    const item = { iun: "pk4", reworkId: "sk4", status: "DONE" };

    try {
      await dynamo.updateRework(item, ["IN_PROGRESS"]);
      expect.fail("Doveva rilanciare l'errore");
    } catch (err) {
      expect(err.name).to.equal("OtherError");
      expect(sendStub.calledOnce).to.be.true;
    }
  });

  it("valida: lancia se mancano i campi obbligatori (status)", async () => {
    const item = { iun: "pk5", reworkId: "sk5" }; // manca status
    try {
      await dynamo.updateRework(item, ["READY"]);
      expect.fail("Doveva lanciare per campi mancanti");
    } catch (err) {
      expect(err.message).to.match(/mandatory/i);
    }
  });

  it("valida: lancia se timelineElementIds è presente ma manca category", async () => {
    const item = { iun: "pk6", reworkId: "sk6", status: "READY", timelineElementIds: ["t1"] };
    try {
      await dynamo.updateRework(item, ["CREATED"]);
      expect.fail("Doveva lanciare per vincolo timelineElementIds/category");
    } catch (err) {
      expect(err.message).to.match(/timelineElementIds needs category/i);
    }
  });

  it("accetta expectedStates come array vuoto → nessuna ConditionExpression", async () => {
    const item = { iun: "pk7", reworkId: "sk7", status: "READY" };
    const res = await dynamo.updateRework(item, []);

    expect(res).to.deep.equal({ ok: true });
    const sent = UpdateCommandCtorStub.firstCall.args[0];
    expect(sent.ConditionExpression).to.be.undefined;
  });

  it("popola solo i campi opzionali presenti", async () => {
    const item = { iun: "pk8", reworkId: "sk8", status: "IN_PROGRESS" };
    const res = await dynamo.updateRework(item, ["READY"]);
    expect(res).to.deep.equal({ ok: true });

    const sent = UpdateCommandCtorStub.firstCall.args[0];
    expect(sent.UpdateExpression).to.not.include("#error = :error");
    expect(sent.UpdateExpression).to.not.include("#category = :category");
    expect(sent.UpdateExpression).to.not.include("#timelineElementIds = :timelineElementIds");
  });

    it("OK: updateRequestRework con status OK", async () => {
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

    const res = await dynamo.updateRequestRework(item, reworkEntity);
    expect(res).to.deep.equal({ ok: true });
    const sent = UpdateCommandCtorStub.firstCall.args[0];
    expect(sent.TableName).to.equal(TABLE_NAME);
    expect(sent.Key).to.deep.equal({ iun: "pkOK", reworkId: "skOK" });
    expect(sent.UpdateExpression).to.include("#status = :ready");
    expect(sent.ExpressionAttributeValues[":newExpectedStatusCodes"]).to.deep.equal(["RECAG001C"]);
    expect(sent.ExpressionAttributeValues[":newDeliveryFailureCause"]).to.equal("M01");
    expect(sendStub.calledOnce).to.be.true;
  });

  it("KO: updateRequestRework con status KO", async () => {
    const item = {
      updateRequest: {
        expectedStatusCodes: ["RECAG001C"],
        expectedDeliveryFailureCause: "M01",
        status: "KO",
        error: ["err"]
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

    const res = await dynamo.updateRequestRework(item, reworkEntity);
    expect(res).to.deep.equal({ ok: true });
    const sent = UpdateCommandCtorStub.firstCall.args[0];
    expect(sent.UpdateExpression).to.include("#status = :ready");
    expect(sent.ExpressionAttributeValues[":updateRequestKo"][0].status).to.equal("KO");
    expect(sent.ExpressionAttributeValues[":updateRequestKo"][0].error).to.deep.equal(["err"]);
    expect(sendStub.calledOnce).to.be.true;
  });

  it("lancia errore se mancano i campi obbligatori", async () => {
    try {
      await dynamo.updateRequestRework({ iun: "x", reworkId: "y" });
      expect.fail("Doveva lanciare");
    } catch (err) {
      expect(err.message).to.match(/mandatory/i);
    }
  });

  it("lancia errore su status non supportato", async () => {
    const item = {
      updateRequest: {
        expectedStatusCodes: ["RECAG001C"],
        expectedDeliveryFailureCause: "M01",
        status: "TEST",
        error: ["err"]
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
    try {
      await dynamo.updateRequestRework(item, reworkEntity);
      expect.fail("Doveva lanciare");
    } catch (err) {
      expect(err.message).to.match(/Not Supported status for UPDATE_REQUEST operation: TEST/i);
    }
  });
});
