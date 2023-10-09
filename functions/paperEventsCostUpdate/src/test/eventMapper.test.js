const { expect } = require("chai");
const fs = require("fs");

const { mapEvents } = require("../app/lib/eventMapper");

describe("event mapper tests", function () {
  const iun = "VWKQ-WQNT-VJZG-202308-K-1";
  const eventType = "UPDATE_COST_PHASE_EVENT";

  it("test SEND_ANALOG_DOMICILE ATTEMPT_0 mapping", async () => {
    const eventJSON = fs.readFileSync(
      "./src/test/events/eventMapper.send_analog_domicile.json"
    );
    let event = JSON.parse(eventJSON);

    const events = [event];

    const res = await mapEvents(events);

    expect(res).length(1);

    let body = JSON.parse(res[0].MessageBody);
    expect(body.iun).equal(iun);
    expect(body.recIndex).equal("0");
    expect(body.notificationStepCost).equal("926");
    expect(body.eventTimestamp).equal("2023-08-09T01:23:56.803455499Z");
    expect(body.eventStorageTimestamp).equal("2023-08-09T01:23:56.803455499Z");
    expect(body.updateCostPhase).equal("SEND_ANALOG_DOMICILE_ATTEMPT_0");

    expect(res[0].MessageAttributes.publisher.StringValue).equal(
      "deliveryPush"
    );
    expect(res[0].MessageAttributes.iun.StringValue).equal(iun);
    expect(res[0].MessageAttributes.eventType.StringValue).equal(eventType);
  });

  it("test SEND_ANALOG_DOMICILE ATTEMPT_1 and different recIndex mapping", async () => {
    const eventJSON = fs.readFileSync(
      "./src/test/events/eventMapper.send_analog_domicile.json"
    );
    let event = JSON.parse(eventJSON);

    // change ATTEMP to 1
    event.dynamodb.NewImage.timelineElementId.S =
      event.dynamodb.NewImage.timelineElementId.S.replace(
        "ATTEMPT_0",
        "ATTEMPT_1"
      );

    // change recIndex to 1
    event.dynamodb.NewImage.details.M.recIndex.N = "1";

    const events = [event];

    const res = await mapEvents(events);

    expect(res).length(1);

    let body = JSON.parse(res[0].MessageBody);
    expect(body.iun).equal(iun);
    expect(body.recIndex).equal("1");
    expect(body.notificationStepCost).equal("926");
    expect(body.eventTimestamp).equal("2023-08-09T01:23:56.803455499Z");
    expect(body.eventStorageTimestamp).equal("2023-08-09T01:23:56.803455499Z");
    expect(body.updateCostPhase).equal("SEND_ANALOG_DOMICILE_ATTEMPT_1");

    expect(res[0].MessageAttributes.publisher.StringValue).equal(
      "deliveryPush"
    );
    expect(res[0].MessageAttributes.iun.StringValue).equal(iun);
    expect(res[0].MessageAttributes.eventType.StringValue).equal(eventType);
  });

  it("test SEND_SIMPLE_REGISTERED_LETTER mapping", async () => {
    // ...
  });

  it("test unmapped event", async () => {
    const eventJSON = fs.readFileSync(
      "./src/test/events/eventMapper.send_analog_domicile.json"
    );
    let event = JSON.parse(eventJSON);

    // specify an unsupported category
    event.dynamodb.NewImage.category.S = "UNSUPPORTED_CATEGORY";

    const events = [event];

    const res = await mapEvents(events);

    expect(res).length(0);
  });
});
