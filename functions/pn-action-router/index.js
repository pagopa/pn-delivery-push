const { handleEvent } = require("./src/app/eventHandler.js");

async function handler(event, context) {
  console.info("New Execution scheduled ", event);
  return await handleEvent(event, context);
}

exports.handler = handler;