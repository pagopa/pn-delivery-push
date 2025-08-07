const { extractKinesisData } = require("./lib/kinesis.js");
const { mapEvents } = require("./lib/eventMapper.js");
const { persistEvents } = require("./lib/repository.js");
const { isPersistenceEnabled } = require("./lib/utils.js");
const { insertAction } = require("./lib/client.js");

exports.handleEvent = async (event) => {
  const defaultPayload = {
    batchItemFailures: [],
  };

  const cdcEvents = extractKinesisData(event);
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length == 0) {
    console.log("No events to process");
    return defaultPayload;
  }

  const processedItems = await mapEvents(cdcEvents);
  if (processedItems.length == 0) {
    console.log("No events to persist");
    return defaultPayload;
  }

  console.log(`Items to persist`, processedItems);

  let persistSummary;
  if(isPersistenceEnabled()) {
    persistSummary = await persistEvents(processedItems); // actually produce changes to DB
  } else {
    console.log("Persistence is disabled, proceeding with action-manager API");
    persistSummary = await insertAction(processedItems); // just call action-manager API
  }

  console.log("Persist summary", persistSummary);
  console.log(`Inserted ${persistSummary.insertions} records`);

  if (persistSummary.errors.length > 0) {
    console.error(
      `Execution finished with ${persistSummary.errors.length} errors`,
      persistSummary.errors
    );
    defaultPayload.batchItemFailures = persistSummary.errors.map((i) => {
      return { itemIdentifier: i.kinesisSeqNumber };
    });
  }

  return defaultPayload;
};
