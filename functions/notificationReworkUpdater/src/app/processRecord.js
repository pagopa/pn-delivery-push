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

  const item = { iun, reworkId, status: nextStatus };

  if (message.error !== undefined) item.error = message.error;

  return { item, expectedStates };
};

const processUpdateRecord = async (message) => {
  const { iun, reworkId, updateValidationStatus } = message || {};

  if (!iun || !reworkId) {
    throw new Error("Missing required fields: iun, reworkId");
  }

  const updateRequestElement = {
    date: new Date().toISOString(),
    status: updateValidationStatus,
    expectedStatusCodes: message.expectedStatusCodes,
    expectedDeliveryFailureCause: message.expectedDeliveryFailureCause
  };

  if (updateValidationStatus === "KO") {
    updateRequestElement.error = message.error;
  }

  const item = {
    iun,
    reworkId,
    status: "READY",
    updateRequest: [updateRequestElement]
  };

  if (updateValidationStatus === "OK") {
    item.deliveryFailureCause = message.deliveryFailureCause;
    item.expectedStatusCodes = message.expectedStatusCodes;
  }

  return { item };
};

module.exports = { processRecord, processUpdateRecord };
