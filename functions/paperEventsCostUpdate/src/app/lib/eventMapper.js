const { parseKinesisObjToJsonObj } = require("./utils");

const TABLES = {
  TIMELINES: "pn-Timelines",
};

const allowedTimelineCategories = [
  "SEND_SIMPLE_REGISTERED_LETTER",
  "SEND_ANALOG_DOMICILE",
];

function extractRecIdsFromTimelineId(timelineElementId) {
  return timelineElementId.split("RECINDEX_")[1];
}

exports.mapEvents = async (events) => {
  const filteredEvents = events.filter((e) => {
    // INSERT events from TIMELINES table, with the allowed categories
    return (
      e.eventName == "INSERT" &&
      e.tableName == TABLES.TIMELINES &&
      e.dynamodb.NewImage.category &&
      allowedTimelineCategories.indexOf(e.dynamodb.NewImage.category.S) >= 0
    );
  });

  let result = [];

  for (const filteredEvent of filteredEvents) {
    let timelineObj = parseKinesisObjToJsonObj(filteredEvent.dynamodb.NewImage);

    let resultElement = {
      iun: timelineObj.iun,
      // ...
    };

    const category = timelineObj.category;

    switch (category) {
      // ...
      case "SEND_SIMPLE_REGISTERED_LETTER":
        // ...
        break;
      case "SEND_ANALOG_DOMICILE":
        // ...
        break;
      default:
        console.log(`Category ${category} not supported`);
        break;
    }

    result.push(resultElement);
  }
  return result;
};
