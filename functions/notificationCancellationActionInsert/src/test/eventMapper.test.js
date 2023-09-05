const { expect } = require("chai");
const fs = require("fs");

const { mapEvents } = require("../app/lib/eventMapper");

describe("event mapper tests", function () {
  it("test NOTIFICATION_CANCELLATION_REQUEST", async () => {
    const eventJSON = fs.readFileSync("./src/test/eventMapper.timeline.json");
    const event = JSON.parse(eventJSON);
    const events = [event];

    const res = await mapEvents(events);

    expect(res[0].opType).equal("INSERT_ACTION_FUTUREACTION");

    expect(res[0].type).equal("NOTIFICATION_CANCELLATION");
    expect(res[0].timelineId).equal(
      "notification_cancellation_request.IUN_XLDW-MQYJ-WUKA-202302-A-1"
    );
    expect(res[0].iun).equal("XLDW-MQYJ-WUKA-202302-A-1");
    expect(res[0].actionId).equal(
      "notification_cancellation_iun_XLDW-MQYJ-WUKA-202302-A-1"
    );
  });
});
