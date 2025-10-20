const { updateStatusAndErrors } = require("./dynamo");
const { processRecord } = require("./processRecord");

const NOTIFICATION_REWORKS_DYNAMO_TABLENAME = process.env.NOTIFICATION_REWORKS_DYNAMO_TABLENAME;

async function handleEvent(timelineElement) {
  if (!timelineElement.Records) {
    console.warn("[NOTIFICATION_REWORK]", "No Records to process");
    return { message: "no items found to be updated" };
  }

  let found = false;

  for (const record of timelineElement.Records) {
    let message = JSON.parse(record.body);

    if (message.operationType === "ERROR") {
      const { iun, reworkId, errors } = message;
      if (!iun || !reworkId || !Array.isArray(errors)) {
        console.error("[NOTIFICATION_REWORK] missing field for update ERROR");
        continue;
      }
    found = true;
    await updateStatusAndErrors(
      NOTIFICATION_REWORKS_DYNAMO_TABLENAME,
      iun,
      reworkId,
      errors
    );
    } else if (message.operationType === "UPDATE") {
      found = true;
    await processRecord(message);
    }
  }

  if (!found) {
    return { message: "no items found to be updated" };
  }

  return { message: "items updated" };
}

module.exports = { handleEvent };