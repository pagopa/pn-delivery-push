const putNotificationRework = require("./putNotificationRework.js");

const processRecord = async (message) => {
  const { iun, category, reworkId } = message;

  console.log(`Processing record for notification rework ${iun} with category ${category} and reworkId ${reworkId}`);

  switch (message.category) {
    case "NOTIFICATION_TIMELINE_REWORKED":
      await putNotificationRework.updateRework(
        message,
        "CREATED",
        "READY",
        true
      );
      break;
    case "SEND_ANALOG_PROGRESS":
      await putNotificationRework.updateRework(
        message,
        "READY",
        "IN_PROGRESS",
        false
      );
      break;
    case "SEND_ANALOG_FEEDBACK":
      await putNotificationRework.updateRework(
        message,
        ["IN_PROGRESS", "READY"],
        "DONE",
        false
      );
      break;
    default:
      throw new Error(`Category ${message.category} not managed`);
  }
};

module.exports = { processRecord };