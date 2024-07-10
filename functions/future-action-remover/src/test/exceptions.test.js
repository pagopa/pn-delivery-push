const {
  InvalidDateException,
  ItemNotFoundException,
  BatchOperationException,
  InvalidItemException
} = require("../app/exceptions");

const { expect } = require("chai");
const { describe, it } = require("mocha");



describe("test InvalidDateException", () => {


  it("should set name", () => {
    const message = "test";
    const exception = new InvalidDateException(message);
    expect(exception.name).to.eq("InvalidDateFound");
    expect(exception.message).to.eq(`Invalid Date Found ${message}`);
  });
});

describe("test ItemNotFoundException", () => {
  it("should set name", () => {
    const key = "KEY";
    const tableName = "TABLE";
    const exception = new ItemNotFoundException(key, tableName);
    expect(exception.name).to.eq("ItemNotFoundException");
    expect(exception.message).to.eq(
      `Item with with id = ${key} not found on table ${tableName}`
    );
  });
});

describe("test InvalidItemException", () => {
  it("should set name", () => {
    const exception = new InvalidItemException();
    expect(exception.name).to.eq("InvalidItemException");   
  });
});

describe("test BatchOperationException", () => {
  it("should set name", () => {
    const operation = "testOperation";
    const mainError = "maiinError";
    const exception = new BatchOperationException(
      operation,
      new Error(mainError)
    );
    expect(exception.name).to.eq("BatchOperationException");
    expect(exception.message).to.eq(
      `Error doing batch ${operation}. Original message ${mainError}`
    );
  });
});
