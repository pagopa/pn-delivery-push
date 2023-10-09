const { parseKinesisObjToJsonObj } = require("./utils");

const TABLES = {
  TIMELINES: "pn-Timelines",
};

exports.mapEvents = async (events) => {
  const filteredEvents = events.filter((e) => {
    return e.eventName == "INSERT" && e.tableName == TABLES.TIMELINES;
  });

  let result = [];

  for (const filteredEvent of filteredEvents) {
    let timelineObj = parseKinesisObjToJsonObj(filteredEvent.dynamodb.NewImage);

    // ...

    let resultElement = {
      // ...
    };

    result.push(resultElement);
  }
  return result;
};
