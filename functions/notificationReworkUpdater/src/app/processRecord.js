// app/processRecord.js
/**
 * Prepara { item, expectedStates } per updateRework.
 *
 * Mappa:
 * - NOTIFICATION_TIMELINE_REWORKED: CREATED -> READY
 * - SEND_ANALOG_PROGRESS, SEND_ANALOG_FEEDBACK:           READY -> IN_PROGRESS
 * - REFINEMENT, ANALOG_WORKFLOW_RECIPIENT_DECEASED:       READY|IN_PROGRESS -> DONE
 *
 * @param {object} message
 * @returns {Promise<{item: object, expectedStates: string[]}>}
 */
const processRecord = async (message) => {
  const { iun, category, reworkId } = message || {};
  if (!iun || !reworkId || !category) {
    throw new Error("Missing required fields: iun, reworkId, category");
  }

  let expectedStates;
  let nextStatus;

  switch (category) {
    case "NOTIFICATION_TIMELINE_REWORKED":
      expectedStates = ["CREATED"];
      nextStatus = "READY";
      break;

    case "SEND_ANALOG_PROGRESS":
      expectedStates = ["READY"];
      nextStatus = "IN_PROGRESS";
      break;

    case "SEND_ANALOG_FEEDBACK":
      expectedStates = ["READY", "IN_PROGRESS"];
      nextStatus = "DONE";
      break;

    default:
      console.warn("[NOTIFICATION_REWORK] Unknown category in processRecord", { iun, reworkId, category });
      throw new Error(`Unknown category: ${category}`);
  }

  const item = { iun, reworkId, status: nextStatus, category };

  if (message.timelineElementIds !== undefined) item.timelineElementIds = message.timelineElementIds;
  if (message.errors !== undefined) item.errors = message.errors;

  return { item, expectedStates };
};

module.exports = { processRecord };
