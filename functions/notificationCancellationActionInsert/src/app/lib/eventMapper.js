const { TABLES } = require("./repository");

const allowedTimelineCategories = ["NOTIFICATION_CANCELLATION_REQUEST"];

function makeNotificationCancellationInsertOp(iun, timelineElementId, event) {
  const now = new Date(); // Get the current date and time
  now.setSeconds(now.getSeconds() + 1); // Add 1 second to the current time
  const scheduleDatetimeString = now.toISOString();
  // datetime until minutes
  const timeslotString = scheduleDatetimeString.slice(0, 16);

  // see pn-delivery-push: it.pagopa.pn.deliverypush.service.impl.SchedulerServiceImpl#scheduleEvent
  const op = {
    // keys for two tables
    actionId: `notification_cancellation_iun_${iun}`,
    timeslot: timeslotString,
    // others
    iun: iun, // GSI in both tables
    type: "NOTIFICATION_CANCELLATION",
    notBefore: scheduleDatetimeString,
    timelineId: timelineElementId, // not actually used
    // no details and not recIdx
    // op
    opType: "INSERT_ACTION_FUTUREACTION",
    kinesisSeqNumber: event.kinesisSeqNumber,
  };

  return op;
}

async function mapPayload(event) {
  const dynamoDbOps = [];

  const iun = event.dynamodb.NewImage.iun.S;
  const timelineElementId = event.dynamodb.NewImage.timelineElementId.S;

  // we con't check for category, since we previously filtered if in mapEvents()
  dynamoDbOps.push(
    makeNotificationCancellationInsertOp(iun, timelineElementId, event)
  );

  // returns an array of ops, empty in case of problems
  return dynamoDbOps;
}

exports.mapEvents = async (events) => {
  const filteredEvents = events.filter((e) => {
    return (
      e.eventName == "INSERT" &&
      e.tableName == TABLES.TIMELINES &&
      e.dynamodb.NewImage.category &&
      allowedTimelineCategories.indexOf(e.dynamodb.NewImage.category.S) >= 0
    );
  });
  let ops = [];

  for (let filteredEvent of filteredEvents) {
    const dynamoDbOps = await mapPayload(filteredEvent);
    ops = ops.concat(dynamoDbOps); // concatenate the arrays
  }
  return ops;
};
