const { expect } = require("chai");

const {
  generateKoResponse,
  generateOkResponse,
} = require("../app/responses.js");

describe("responses tests", () => {
  it("generateOkResponse for timeout", () => {
    const result = generateOkResponse(true);

    expect(result).to.not.be.null;
    expect(result.statusCode).to.be.eq(200);
    expect(result).to.have.property("body");
    expect(result.body).to.be.eq(JSON.stringify({ reason: "TIMEOUT REACHED" }));
  });

  it("generateOkResponse for timeout", () => {
    const result = generateOkResponse(false);

    expect(result).to.not.be.null;
    expect(result.statusCode).to.be.eq(200);
    expect(result).to.have.property("body");
    expect(result.body).to.be.eq(JSON.stringify({}));
  });

  it("generateKoResponse ", () => {
    const result = generateKoResponse(new Error("TEST ERROR"));

    expect(result).to.not.be.null;
    expect(result.statusCode).to.be.eq(500);
  });
});
