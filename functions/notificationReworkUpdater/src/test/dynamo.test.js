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
    expect(sent.Key).to.deep.equal({ pk: "pk1", sk: "sk1" });
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
      errors: ["e1", "e2"]
    };

    const res = await dynamo.updateRework(item, ["CREATED"]);
    expect(res).to.deep.equal({ ok: true });

    const sent = UpdateCommandCtorStub.firstCall.args[0];
    expect(sent.ConditionExpression).to.be.undefined;
    expect(sent.UpdateExpression).to.include("#s = :newStatus");
    expect(sent.UpdateExpression).to.include("#errors = :errors");
    expect(sent.ExpressionAttributeValues[":newStatus"]).to.equal("ERROR");
    expect(sent.ExpressionAttributeValues[":errors"]).to.deep.equal(["e1", "e2"]);
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
      expect(err.message).to.match(/obbligatori/i);
    }
  });

  it("valida: lancia se timelineElementIds è presente ma manca category", async () => {
    const item = { iun: "pk6", reworkId: "sk6", status: "READY", timelineElementIds: ["t1"] };
    try {
      await dynamo.updateRework(item, ["CREATED"]);
      expect.fail("Doveva lanciare per vincolo timelineElementIds/category");
    } catch (err) {
      expect(err.message).to.match(/timelineElementIds richiede category/i);
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
    expect(sent.UpdateExpression).to.not.include("#errors = :errors");
    expect(sent.UpdateExpression).to.not.include("#category = :category");
    expect(sent.UpdateExpression).to.not.include("#timelineElementIds = :timelineElementIds");
  });
});
