const { expect } = require("chai");
const sinon = require("sinon");
const fs = require("fs");
const jsonwebtoken = require("jsonwebtoken");

const { eventHandler } = require("../app/eventHandler.js");
const retrieverPdndJwks = require("../app/retrieverPdndJwks");
const dynamoFunctions = require("../app/dynamoFunctions.js");
const event = require("../../event.json");
const eventPdndFromJson = require("../../event-PDND.json");
const eventPdnd = {
  ...eventPdndFromJson,
  headers: {
    ...eventPdndFromJson,
    Authorization:
      "Bearer eyJ0eXAiOiJhdCtqd3QiLCJhbGciOiJSUzI1NiIsInVzZSI6InNpZyIsImtpZCI6IjMyZDhhMzIxLTE1NjgtNDRmNS05NTU4LWE5MDcyZjUxOWQyZCJ9.eyJhdWQiOiJodHRwczovL2FwaS5kZXYucG4ucGFnb3BhLml0Iiwic3ViIjoiYmQ1ZWNiNzgtZTFkNC00MzIyLTkyMWMtNmZlMGVhYTQyN2Q1IiwibmJmIjoxNjgxMjE1MDYwLCJwdXJwb3NlSWQiOiIyMDk5OWEwZi1lYzQwLTQxYzctOWZkZC05ZDNhZDA3OWFkODEiLCJpc3MiOiJ1YXQuaW50ZXJvcC5wYWdvcGEuaXQiLCJleHAiOjE2ODEyMTg2NjAsImlhdCI6MTY4MTIxNTA2MCwiY2xpZW50X2lkIjoiYmQ1ZWNiNzgtZTFkNC00MzIyLTkyMWMtNmZlMGVhYTQyN2Q1IiwianRpIjoiNjUxYjIwNWMtZTc2ZS00NGQzLWIwMjgtNDVhOTlhZTQ2ZTE3In0.27kvqi7P7dXFp7m8o7Eu_uuoZIAP4zMkeNQbp1S-cmwGe4ceVBz3N-172YizmUeMTsg4DwRGdUegGQdc_wlXOUO445r3-nf-qNu01SafAYD0d9rpCkqno3vm5Bd2OksDexH8H4X97A2Ygp_YI9qrnlcCKjmYK0Qa6zoKGqptL_-Oxe7CzSuTpDI2TTXBgCNg90gfnHkzxz3RUNMaZ3xf3p-BNUt4-kWd7jGdnvualV1yNdBNUcviylWHfxbDR-v0zdrmvr-aVZYb-SX0WVLAQwiAX_0EzCnpzDEoncV_1bB_jhJHNSjdO_-LRnF6K3SxUuSsaYon7HkP3A_JkFq8GQ",
  },
};
const {
  mockPaAggregationFound,
  mockAggregateFound,
  mockEventTokenNull,
  mockVirtualKey,
  mockDecodedJwt,
} = require("./mocks.js");

describe("eventHandler test ", function () {
  const jwksFromPdnd = JSON.parse(
    fs.readFileSync("./src/test/jwks-mock/interop-pagopa-jwks.json", {
      encoding: "utf8",
    })
  );
  let tokenVerify;
  let getApiKeyByIndexStub;
  let getPaAggregationByIdStub;
  let getPaAggregateByIdStub;

  before(() => {
    tokenVerify = sinon.stub(jsonwebtoken, "verify");
    sinon.stub(retrieverPdndJwks, "getJwks").callsFake(() => jwksFromPdnd);
    sinon.stub(process, "env").value({
      PDND_ISSUER: "uat.interop.pagopa.it",
      PDND_AUDIENCE: "https://api.dev.pn.pagopa.it",
    });
    getApiKeyByIndexStub = sinon.stub(dynamoFunctions, "getApiKeyByIndex");
    getPaAggregationByIdStub = sinon.stub(
      dynamoFunctions,
      "getPaAggregationById"
    );
    getPaAggregateByIdStub = sinon.stub(dynamoFunctions, "getPaAggregateById");
  });

  after(() => {
    sinon.restore();
  });

  it("apiKeyBlocked", async () => {
    getApiKeyByIndexStub.callsFake(() => ({
      ...mockVirtualKey,
      status: "BLOCKED",
    }));
    const res = await eventHandler(event, null);
    expect(res.policyDocument.Statement[0].Effect).equal("Deny");
    expect(res.usageIdentifierKey).to.be.undefined;
  });

  it("iam policy ok", async () => {
    getApiKeyByIndexStub.callsFake(() => mockVirtualKey);
    getPaAggregationByIdStub.callsFake(() => mockPaAggregationFound);
    getPaAggregateByIdStub.callsFake(() => mockAggregateFound);

    const res = await eventHandler(event, null);
    expect(res.context.sourceChannelDetails).equals("NONINTEROP");
    expect(res.usageIdentifierKey).equal(mockAggregateFound.AWSApiKey);
    expect(res.context.cx_groups).equal(mockVirtualKey.groups.join());
  });

  it("iam policy KO - pdnd", async () => {
    getApiKeyByIndexStub.callsFake(() => mockVirtualKey);
    getPaAggregationByIdStub.callsFake(() => mockPaAggregationFound);
    getPaAggregateByIdStub.callsFake(() => mockAggregateFound);

    const res = await eventHandler(eventPdnd, null);
    expect(res.policyDocument.Statement[0].Effect).equal("Deny");
    expect(res.usageIdentifierKey).to.be.undefined;
  });

  it("iam policy ok - pdnd", async () => {
    tokenVerify.returns("token.token.token");
    getApiKeyByIndexStub.callsFake(() => ({ ...mockVirtualKey, pdnd: true }));
    getPaAggregationByIdStub.callsFake(() => mockPaAggregationFound);
    getPaAggregateByIdStub.callsFake(() => mockAggregateFound);

    const res = await eventHandler(eventPdnd, null);
    expect(res.context.sourceChannelDetails).equals("INTEROP");
    expect(res.context.uid).equal("PDND-" + mockDecodedJwt.client_id);
  });

  it("pdnd error thrown", async () => {
    tokenVerify.throws();
    getApiKeyByIndexStub.callsFake(() => ({ ...mockVirtualKey, pdnd: true }));
    getPaAggregationByIdStub.callsFake(() => mockPaAggregationFound);
    getPaAggregateByIdStub.callsFake(() => mockAggregateFound);

    const res = await eventHandler(event, null);
    expect(res.policyDocument.Statement[0].Effect).equal("Deny");
    expect(res.usageIdentifierKey).to.be.undefined;
  });

  it("pdnd error thrown token null", async () => {
    tokenVerify.throws();
    getApiKeyByIndexStub.callsFake(() => ({ ...mockVirtualKey, pdnd: true }));
    getPaAggregationByIdStub.callsFake(() => mockPaAggregationFound);
    getPaAggregateByIdStub.callsFake(() => mockAggregateFound);

    const res = await eventHandler(mockEventTokenNull, null);
    expect(res.policyDocument.Statement[0].Effect).equal("Deny");
    expect(res.usageIdentifierKey).to.be.undefined;
  });

  it("error thrown", async () => {
    getApiKeyByIndexStub.throws();
    const res = await eventHandler(event, null);
    expect(res.policyDocument.Statement[0].Effect).equal("Deny");
    expect(res.usageIdentifierKey).to.be.undefined;
  });
});
