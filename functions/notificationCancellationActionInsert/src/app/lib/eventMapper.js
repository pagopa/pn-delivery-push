const allowedTimelineCategories = ["NOTIFICATION_CANCELLATION_REQUEST"];

async function mapPayload(event) {
  // ...
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
    ops = ops.concat(dynamoDbOps);
  }
  return ops;
};
