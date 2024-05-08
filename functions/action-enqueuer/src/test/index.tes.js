const { expect } = require("chai");
const sinon = require("sinon");

const dynamoFunctions = require("../app/dynamoFunctions");
const lambda = require("../../index");
const event = require("../../event.json");
const {
  mockVirtualKey,
  mockPaAggregationFound,
  mockAggregateFound,
} = require("./mocks");

describe("index tests", function () {
  let getApiKeyByIndexStub;

  before(() => {
    sinon.stub(process, "env").value({
      PDND_ISSUER: "uat.interop.pagopa.it",
      PDND_AUDIENCE: "https://api.dev.pn.pagopa.it",
    });
    getApiKeyByIndexStub = sinon.stub(dynamoFunctions, "getApiKeyByIndex");
    sinon
      .stub(dynamoFunctions, "getPaAggregationById")
      .callsFake(() => mockPaAggregationFound);
    sinon
      .stub(dynamoFunctions, "getPaAggregateById")
      .callsFake(() => mockAggregateFound);
  });

  after(() => {
    sinon.restore();
  });

  it("test Ok", async () => {
    getApiKeyByIndexStub.callsFake(() => mockVirtualKey);
    const res = await lambda.handler(event, null);
    expect(res.usageIdentifierKey).equal(mockAggregateFound.AWSApiKey);
    expect(res.context.cx_groups).equal(mockVirtualKey.groups.join());
  });

  it("test fail", async () => {
    getApiKeyByIndexStub.throws();
    const res = await lambda.handler(event, null);
    expect(res.policyDocument.Statement[0].Effect).equal("Deny");
  });
});
