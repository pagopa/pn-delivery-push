class SQSServiceException extends Error {
  constructor(e) {
    super(`${e.message}`);
    this.name = "SQSServiceException";
  }
}

class TimeoutException extends Error {
  constructor(e) {
    super(`${e.message}`);
    this.name = "TimeoutException";
  }
}

module.exports = {
  SQSServiceException,
  TimeoutException,
};
