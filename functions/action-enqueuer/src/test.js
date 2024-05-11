const config = require("config");
//return handleEvent(event, context);

console.log("AAAA!", config.get("MAX_SQS_BATCH_SIZE"));
