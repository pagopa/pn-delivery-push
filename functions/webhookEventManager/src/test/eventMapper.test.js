const { expect } = require("chai");
const fs = require("fs");

const { mapEvents } = require("../app/lib/eventMapper");

describe("event mapper tests", function () {

  
  it("test mapping", async () => {
    const eventJSON = fs.readFileSync("./src/test/eventMapper.timeline.json");
    let event = JSON.parse(eventJSON);
    event = setCategory(event, "REQUEST_REFUSED");

    const events = [event];

    const res = await mapEvents(events);

    console.log(res[0]);

    expect(res[0].payload.iun).equal("abcd");
    expect(res[0].payload.paId).equal("026e8c72-7944-4dcd-8668-f596447fec6d");
    expect(res[0].payload.timelineId).equal("notification_viewed_creation_request;IUN_XLDW-MQYJ-WUKA-202302-A-1;RECINDEX_1");
    expect(res[0].payload.type).equal("REGISTER_EVENT");

    expect(res[0].header.publisher).equal("deliveryPush");
    expect(res[0].header.iun).equal("abcd");
    expect(res[0].header.eventType).equal("WEBHOOK_ACTION_GENERIC");

    console.log('OK');
  
  });

});

function setCategory(event, category) {
  event.dynamodb.NewImage.category = {
    S: category,
  };
  return event;
}
