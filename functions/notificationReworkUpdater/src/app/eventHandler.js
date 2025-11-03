const { updateRework } = require("./dynamo");
const { processRecord } = require("./processRecord");

exports.handler = async (event) => {
  const failures = [];

  for (const rec of event.Records ?? []) {
    const messageId = rec.messageId;

    let msg;
    try {
      msg = JSON.parse(rec.body);
    } catch (e) {
      console.warn("[NOTIFICATION_REWORK] invalid JSON", e);
      failures.push({ itemIdentifier: messageId });
      continue;
    }

    try {
      if (msg.operationType === "ERROR") {
        const { iun, reworkId, errors } = msg;
        if (!iun || !reworkId || !Array.isArray(errors)) {
          console.warn("[NOTIFICATION_REWORK] missing fields for ERROR", { iun, reworkId });
          failures.push({ itemIdentifier: messageId });
          continue;
        }
        const res = await updateRework({ iun, reworkId, status: "ERROR", errors }, null);
        if (res?.ok === false && res?.reason === "CONDITION_FAILED") {
          console.warn("[NOTIFICATION_REWORK] unexpected condition fail on ERROR", { iun, reworkId });
        }
      } else if (msg.operationType === "UPDATE") {
        const { item, expectedStates } = await processRecord(msg);
        const res = await updateRework(item, expectedStates);
        if (res?.ok === false && res?.reason === "CONDITION_FAILED") {
          console.warn(
            "[NOTIFICATION_REWORK] conditional check failed (no retry)",
            { iun: item.iun, reworkId: item.reworkId, expectedStates }
          );
        }
      } else {
        console.warn("[NOTIFICATION_REWORK] unknown operationType", { operationType: msg.operationType });
        failures.push({ itemIdentifier: messageId });
      }
    } catch (e) {
      console.error("[NOTIFICATION_REWORK] fatal error on record", e);
      failures.push({ itemIdentifier: messageId });
    }
  }

  return { batchItemFailures: failures };
};
