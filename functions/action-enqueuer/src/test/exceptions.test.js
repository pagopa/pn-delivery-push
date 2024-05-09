const { SQSServiceException, TimeoutException } = require("../app/exceptions");

const { expect } = require("chai");
const { describe, it } = require("mocha");

describe("test SQSServiceException", () => {
  it("should set name", () => {
    const message = "test";
    const exception = new SQSServiceException(message);
    expect(exception.name).to.eq("SQSServiceException");
  });
});

describe("test TimeoutException", () => {
  it("should set name", () => {
    const key = "KEY";
    const tableName = "TABLE";
    const exception = new TimeoutException(key, tableName);
    expect(exception.name).to.eq("TimeoutException");
  });
});
