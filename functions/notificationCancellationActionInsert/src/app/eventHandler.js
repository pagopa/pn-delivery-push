const { extractKinesisData } = require("./lib/kinesis.js");
const { mapEvents } = require("./lib/eventMapper.js");

exports.handleEvent = async (event) => {
  const cdcEvents = extractKinesisData(event);
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length == 0) {
    console.log("No events to process");
    return {
      batchItemFailures: [],
    };
  }

  const processedItems = await mapEvents(cdcEvents);
  if (processedItems.length == 0) {
    console.log("No events to persist");
    return {
      batchItemFailures: [],
    };
  }

  console.log(`Items to persist`, processedItems);
  // ...
  return {
    batchItemFailures: [],
  };
};
