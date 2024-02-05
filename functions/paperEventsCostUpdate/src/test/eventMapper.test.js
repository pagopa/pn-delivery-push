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
    const eventJSON = fs.readFileSync(
      "./src/test/events/eventMapper.send_analog_domicile.json"
    );
    let event = JSON.parse(eventJSON);

    // change category to SEND_SIMPLE_REGISTERED_LETTER
    event.dynamodb.NewImage.category.S = "SEND_SIMPLE_REGISTERED_LETTER";

    // change the timelineElementId
    event.dynamodb.NewImage.timelineElementId.S =
      "SEND_SIMPLE_REGISTERED_LETTER.IUN_" + iun + ".RECINDEX_0";

    const events = [event];

    const res = await mapEvents(events);

    expect(res).length(1);

    let body = JSON.parse(res[0].MessageBody);
    expect(body.iun).equal(iun);
    expect(body.recIndex).equal("0");
    expect(body.notificationStepCost).equal("926");
    expect(body.eventTimestamp).equal("2023-08-09T01:23:56.803455499Z");
    expect(body.eventStorageTimestamp).equal("2023-08-09T01:23:56.803455499Z");
    expect(body.updateCostPhase).equal("SEND_SIMPLE_REGISTERED_LETTER");

    expect(res[0].MessageAttributes.publisher.StringValue).equal(
      "deliveryPush"
    );
    expect(res[0].MessageAttributes.iun.StringValue).equal(iun);
    expect(res[0].MessageAttributes.eventType.StringValue).equal(eventType);
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

  it("test missing fields event", async () => {
    const eventJSON = fs.readFileSync(
      "./src/test/events/eventMapper.send_analog_domicile.json"
    );
    let event = JSON.parse(eventJSON);

    // remove one of the required fields
    let saved = event.dynamodb.NewImage.details.M.recIndex;
    delete event.dynamodb.NewImage.details.M.recIndex;

    let events = [event];

    let res = await mapEvents(events);

    expect(res).length(0);

    // restore the field and remove a different one
    event.dynamodb.NewImage.details.M.recIndex = saved;
    saved = event.dynamodb.NewImage.details.M.analogCost;
    delete event.dynamodb.NewImage.details.M.analogCost;

    events = [event];

    res = await mapEvents(events);

    expect(res).length(0);

    // restore the field and test ok
    event.dynamodb.NewImage.details.M.analogCost = saved;

    events = [event];

    res = await mapEvents(events);

    expect(res).length(1);
  });

  it("test wrong type or missing timelineElementId", async () => {
    const eventJSON = fs.readFileSync(
      "./src/test/events/eventMapper.send_analog_domicile.json"
    );
    let event = JSON.parse(eventJSON);

    // change timelineElementId to a wrong type after saving the original value
    let saved = event.dynamodb.NewImage.timelineElementId.S;
    event.dynamodb.NewImage.timelineElementId.S = 1234;

    let events = [event];

    let res = await mapEvents(events);

    expect(res).length(0);

    // restore and retry
    event.dynamodb.NewImage.timelineElementId.S = saved;

    events = [event];

    res = await mapEvents(events);

    expect(res).length(1);

    // remove timelineElementId
    delete event.dynamodb.NewImage.timelineElementId;

    events = [event];

    res = await mapEvents(events);

    expect(res).length(0);
  });

  it("test SEND_ANALOG_DOMICILE missing ATTEMPT", async () => {
    const eventJSON = fs.readFileSync(
      "./src/test/events/eventMapper.send_analog_domicile.json"
    );
    let event = JSON.parse(eventJSON);

    // remove ATTEMPT_0 from timelineElementId
    event.dynamodb.NewImage.timelineElementId.S =
      event.dynamodb.NewImage.timelineElementId.S.replace("ATTEMPT_0", "");

    const events = [event];

    const res = await mapEvents(events);

    expect(res).length(0);
  });

  it("test SEND_ANALOG_DOMICILE ATTEMPT_0 with vat", async () => {
    const eventJSON = fs.readFileSync(
      "./src/test/events/eventMapper.send_analog_domicile_vat.json"
    );
    let event = JSON.parse(eventJSON);

    const events = [event];

    const res = await mapEvents(events);

    expect(res).length(1);

    let body = JSON.parse(res[0].MessageBody);
    expect(body.iun).equal(iun);
    expect(body.recIndex).equal("0");
    expect(body.notificationStepCost).equal("1130");
    expect(body.eventTimestamp).equal("2023-08-09T01:23:56.803455499Z");
    expect(body.eventStorageTimestamp).equal("2023-08-09T01:23:56.803455499Z");
    expect(body.updateCostPhase).equal("SEND_ANALOG_DOMICILE_ATTEMPT_0");

    expect(res[0].MessageAttributes.publisher.StringValue).equal(
      "deliveryPush"
    );
    expect(res[0].MessageAttributes.iun.StringValue).equal(iun);
    expect(res[0].MessageAttributes.eventType.StringValue).equal(eventType);
  });
});
