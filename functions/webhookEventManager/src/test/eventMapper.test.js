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

    let body = JSON.parse(res[0].MessageBody);
    expect(body.iun).equal("abcd");
    expect(body.paId).equal("026e8c72-7944-4dcd-8668-f596447fec6d");
    expect(body.timelineId).equal("notification_viewed_creation_request;IUN_XLDW-MQYJ-WUKA-202302-A-1;RECINDEX_1");
    expect(body.type).equal("REGISTER_EVENT");

    expect(res[0].MessageAttributes.publisher.StringValue).equal("deliveryPush");
    expect(res[0].MessageAttributes.iun.StringValue).equal("abcd");
    expect(res[0].MessageAttributes.eventType.StringValue).equal("WEBHOOK_ACTION_GENERIC");

    console.log('OK');
  
  });

});

function setCategory(event, category) {
  event.dynamodb.NewImage.category = {
    S: category,
  };
  return event;
}
