const { expect } = require("chai");
const sinon = require("sinon");
const proxyquire = require("proxyquire");

describe("dynamo.js", () => {
  let docClientStub, UpdateCommandStub, dynamo, sendStub;

  beforeEach(() => {
    sendStub = sinon.stub().resolves();
    docClientStub = { send: sendStub };
    UpdateCommandStub = sinon.stub();

    dynamo = proxyquire("../app/dynamo.js", {
      "@aws-sdk/lib-dynamodb": {
        UpdateCommand: UpdateCommandStub
      },
      "@aws-sdk/client-dynamodb": {},
      "@aws-sdk/lib-dynamodb": {
        DynamoDBDocumentClient: { from: () => docClientStub },
        UpdateCommand: UpdateCommandStub
      }
    });
  });

  afterEach(() => {
    sinon.restore();
  });

  it("updateWithConditionalStatus should send an UpdateCommand with the correct parameters", async () => {
    UpdateCommandStub.returns({});

    const item = {
      iun: "pk1",
      reworkId: "sk1",
      status: "IN_PROGRESS",
      updatedAt: "2024-06-01T12:00:00Z",
      flagInvalidatedIds: ["id1", "id2"]
    };
    await dynamo.updateWithConditionalStatus("TestTable", item, ["CREATED", "IN_PROGRESS"]);

    expect(UpdateCommandStub.calledOnce).to.be.true;
    const params = UpdateCommandStub.firstCall.args[0];
    expect(params.TableName).to.equal("TestTable");
    expect(params.Key).to.deep.equal({ pk: "pk1", sk: "sk1" });
    expect(params.UpdateExpression).to.include("SET #s = :newStatus");
    expect(params.ConditionExpression).to.include("#s IN");
    expect(params.ExpressionAttributeValues[":newStatus"]).to.equal("IN_PROGRESS");
    expect(params.ExpressionAttributeValues[":updatedAt"]).to.equal("2024-06-01T12:00:00Z");
    expect(params.ExpressionAttributeValues[":invalidatedIds"]).to.deep.equal(["id1", "id2"]);
    expect(sendStub.calledOnce).to.be.true;
  });

  it("updateStatusAndErrors should send an UpdateCommand with the correct parameters", async () => {
    UpdateCommandStub.returns({});
    await dynamo.updateStatusAndErrors("TestTable", "pk2", "sk2", ["err1", "err2"]);

    expect(UpdateCommandStub.calledOnce).to.be.true;
    const params = UpdateCommandStub.firstCall.args[0];
    expect(params.TableName).to.equal("TestTable");
    expect(params.Key).to.deep.equal({ pk: "pk2", sk: "sk2" });
    expect(params.UpdateExpression).to.equal("SET #s = :error, #e = :errors");
    expect(params.ExpressionAttributeValues[":error"]).to.equal("ERROR");
    expect(params.ExpressionAttributeValues[":errors"]).to.deep.equal(["err1", "err2"]);
    expect(sendStub.calledOnce).to.be.true;
  });

  it("handles ConditionalCheckFailedException in updateWithConditionalStatus", async () => {
    UpdateCommandStub.returns({});
    sendStub.rejects({ name: "ConditionalCheckFailedException" });

    const item = {
      iun: "pk3",
      reworkId: "sk3",
      status: "IN_PROGRESS",
      updatedAt: "2024-06-01T12:00:00Z"
    };
    await dynamo.updateWithConditionalStatus("TestTable", item, ["CREATED"]);
    expect(sendStub.calledOnce).to.be.true;
  });

  it("raises other errors in updateWithConditionalStatus", async () => {
    UpdateCommandStub.returns({});
    sendStub.rejects({ name: "OtherError", message: "generic error" });

    const item = {
      iun: "pk4",
      reworkId: "sk4",
      status: "IN_PROGRESS",
      updatedAt: "2024-06-01T12:00:00Z"
    };
    try {
      await dynamo.updateWithConditionalStatus("TestTable", item, ["CREATED"]);
      expect.fail("Doveva rilanciare l'errore");
    } catch (err) {
      expect(err.name).to.equal("OtherError");
    }
  });

  it("relaunch errors in updateStatusAndErrors", async () => {
    UpdateCommandStub.returns({});
    sendStub.rejects({ name: "OtherError", message: "generic error" });

    try {
      await dynamo.updateStatusAndErrors("TestTable", "pk5", "sk5", ["err"]);
      expect.fail("It had to relaunch the error");
    } catch (err) {
      expect(err.name).to.equal("OtherError");
    }
  });
});