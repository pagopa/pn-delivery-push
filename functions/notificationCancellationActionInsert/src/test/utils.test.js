const { expect } = require("chai");
const { nDaysFromNowAsUNIXTimestamp } = require("../app/lib/utils.js");

// test utils
describe("utils tests", function () {
  it("test nDaysFromNowAsUNIXTimestamp", async () => {
    const res = nDaysFromNowAsUNIXTimestamp(1);
    expect(res).equal(Math.floor(Date.now() / 1000) + 86400);
  });

  it("test nDaysFromNowAsUNIXTimestamp 0", async () => {
    const res = nDaysFromNowAsUNIXTimestamp(0);
    expect(res).equal(0);
  });

  it("test nDaysFromNowAsUNIXTimestamp -1", async () => {
    const res = nDaysFromNowAsUNIXTimestamp(-1);
    expect(res).equal(0);
  });
});
