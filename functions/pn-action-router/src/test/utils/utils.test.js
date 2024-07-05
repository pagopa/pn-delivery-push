//2024-07-05T13:15:11.508Z
const { expect } = require("chai");
const { addDaysToDate } = require("../../app/utils/utils.js");

describe("addDateTest", function () {
  it("test Ok", async () => {
    let startDate = "2024-07-25T13:15:11.508011744Z";

    let endDate = addDaysToDate(startDate, 10);

    console.log("EndDate is ",endDate);

    expect(endDate).deep.equals("2024-08-04T13:15:11.508Z");
  });
});