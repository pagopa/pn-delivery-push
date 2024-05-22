class BatchOperationException extends Error {
  constructor(operation, e) {
    super(`Error doing batch ${operation}. Original message ${e.message}`);
    this.name = "BatchOperationException";
    this.stack = e.stack;
  }
}
class ItemNotFoundException extends Error {
  constructor(key, tableName) {
    super(`Item with with id = ${key} not found on table ${tableName}`);
    this.name = "ItemNotFoundException";
  }
}

class InvalidDateException extends Error {
  constructor(d) {
    super(`Invalid Date Found ${d}`);
    this.name = "InvalidDateFound";
  }
}

module.exports = {
  InvalidDateException,
  ItemNotFoundException,
  BatchOperationException,
};
