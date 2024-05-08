const sinon = require("sinon");
const fs = require("fs");
const chaiAsPromised = require("chai-as-promised");
const chai = require("chai");

const { get, isCacheActive } = require("../app/jwksCache");
const retrieverPdndJwks = require("../app/retrieverPdndJwks");

chai.use(chaiAsPromised);
const expect = chai.expect;
const SIX_MINUTES_IN_MS = process.env.CACHE_TTL
  ? Number(process.env.CACHE_TTL) * 1000 + 360000
  : 360000 + 300 * 1000;

describe("test jwksCache", () => {
  const jwksFromPdnd = JSON.parse(
    fs.readFileSync("./src/test/jwks-mock/interop-pagopa-jwks.json", {
      encoding: "utf8",
    })
  );
  let clock;
  let getJwksStub;

  before(() => {
    clock = sinon.useFakeTimers({ now: Date.now(), shouldAdvanceTime: true });
    sinon.stub(process, "env").value({
      PDND_ISSUER: "uat.interop.pagopa.it",
      PDND_AUDIENCE: "https://api.dev.pn.pagopa.it",
      CACHE_TTL: "300",
    });
    getJwksStub = sinon.stub(retrieverPdndJwks, "getJwks");
  });

  after(() => {
    sinon.restore();
    clock.restore();
  });

  it("initialize cache when is empty", async () => {
    getJwksStub.callsFake(() => jwksFromPdnd);
    const jwks = await get();
    expect(jwks.keys).to.be.eql(jwksFromPdnd.keys);
    expect(jwks.expiresOn).not.to.be.undefined;
    expect(jwks.lastUpdate).not.to.be.undefined;
    expect(isCacheActive()).to.be.true;
  });

  it("refresh cache when is expired", async () => {
    getJwksStub.callsFake(() => jwksFromPdnd);
    const jwks = await get();
    const firstExpiresOn = jwks.expiresOn;
    expect(jwks.keys).to.be.eql(jwksFromPdnd.keys);
    expect(firstExpiresOn).not.to.be.undefined;
    expect(jwks.lastUpdate).not.to.be.undefined;

    clock.tick(SIX_MINUTES_IN_MS);

    const now = Date.now();
    const isCacheExpired = firstExpiresOn < now;
    expect(isCacheExpired).to.be.true;

    const newJwks = await get();
    const secondExpiresOn = newJwks.expiresOn;
    expect(firstExpiresOn).to.be.lessThan(secondExpiresOn);
  });

  it("error initializing cache", async () => {
    //First call fails
    getJwksStub.throws();

    expect(get).throws;
  });

  it("error refreshing cache", async () => {
    //First call succeed
    getJwksStub.onCall(0).callsFake(() => jwksFromPdnd);
    // Second call fails
    getJwksStub.onCall(1).throws();

    const jwks = await get();
    const firstExpiresOn = jwks.expiresOn;
    expect(jwks.keys).to.be.eql(jwksFromPdnd.keys);
    expect(firstExpiresOn).not.to.be.undefined;
    expect(jwks.lastUpdate).not.to.be.undefined;

    // advance time to simulate expiration
    clock.tick(SIX_MINUTES_IN_MS);

    // Check expiration
    const now = Date.now();
    const isCacheExpired = firstExpiresOn < now;
    expect(isCacheExpired).to.be.true;

    //Obtain old cache value
    const secondJwks = await get();
    const secondExpiresOn = secondJwks.expiresOn;
    expect(firstExpiresOn).to.be.eql(secondExpiresOn);
  });
});
