const axios = require("axios");
const MockAdapter = require("axios-mock-adapter");
const fs = require("fs");
const chai = require("chai");
const chaiAsPromised = require("chai-as-promised");

const { getJwks } = require("../app/retrieverPdndJwks.js");

chai.use(chaiAsPromised);
const expect = chai.expect;

describe("retrieverPdndJwks", () => {
  const result = fs.readFileSync(
    "./src/test/jwks-mock/interop-pagopa-jwks.json",
    { encoding: "utf8" }
  );
  const jsonResult = JSON.parse(result);
  let mock;

  before(() => {
    mock = new MockAdapter(axios);
  });

  afterEach(() => {
    mock.reset();
  });

  after(() => {
    mock.restore();
  });

  it("success", async () => {
    mock
      .onGet("https://dev.interop.pagopa.it/.well-known/jwks.json")
      .reply(200, jsonResult);

    const response = await getJwks("dev.interop.pagopa.it");
    expect(response).to.be.eq(response);
  });

  it("error", async () => {
    mock
      .onGet("https://dev.interop.pagopa.it/.well-known/jwks.json")
      .reply(500);

    await expect(getJwks("dev.interop.pagopa.it")).to.be.rejectedWith(
      Error,
      "Error in get pub key"
    );
  });
});
