const { extractKinesisData } = require("./lib/kinesis.js");
const { mapEvents } = require("./lib/eventMapper.js");
const { SQSClient, SendMessageBatchCommand } = require("@aws-sdk/client-sqs");

const sqs = new SQSClient({ region: process.env.REGION });
const QUEUE_URL = process.env.QUEUE_URL;

exports.handleEvent = async (event) => {
  // 1. Kinesis data extraction
  const cdcEvents = extractKinesisData(event);
  console.log(`Batch size: ${cdcEvents.length} cdc`);

  if (cdcEvents.length == 0) {
    console.log("No events to process");
    return {
      batchItemFailures: [],
    };
  } else {
    let batchItemFailures = [];
    while (cdcEvents.length > 0) {
      // we process them in batches of 10
      let currentCdcEvents = cdcEvents.splice(0, 10);

      try {
        // 2. event mapping
        let processedItems = await mapEvents(currentCdcEvents);

        // 3. SQS message sending
        if (processedItems.length > 0) {
          let responseError = await sendMessages(processedItems);

          if (responseError.length > 0) {
            console.log(
              "Error in persisting current cdcEvents: ",
              JSON.stringify(currentCdcEvents)
            );
            batchItemFailures = batchItemFailures.concat(
              responseError.map((i) => {
                return { itemIdentifier: i.kinesisSeqNumber };
              })
            );
          }
        } else {
          console.log(
            "No events to persist in current cdcEvents: ",
            JSON.stringify(currentCdcEvents)
          );
        }
      } catch (exc) {
        console.log(
          "Error in persisting current cdcEvents: ",
          currentCdcEvents
        );
        batchItemFailures = batchItemFailures.concat(
          currentCdcEvents.map((i) => {
            return { itemIdentifier: i.kinesisSeqNumber };
          })
        );
      }
    }
    if (batchItemFailures.length > 0) {
      console.log("process finished with some errors!");
    }
    return {
      batchItemFailures: batchItemFailures,
    };
  }
};

/**
 * send the messages to SQS in batch
 *
 * @param {*} messages array of mapped messages to send
 * @returns
 */
async function sendMessages(messages) {
  let error = [];
  try {
    console.log(
      "Proceeding to send " + messages.length + " messages to " + QUEUE_URL
    );
    const input = {
      Entries: messages,
      QueueUrl: QUEUE_URL,
    };

    console.log("Sending batch message: %j", input);

    const command = new SendMessageBatchCommand(input);
    const response = await sqs.send(command);

    if (response.Failed && response.Failed.length > 0) {
      console.log(
        "error sending some message totalErrors:" + response.Failed.length
      );

      error = error.concat(
        response.Failed.map((i) => {
          return { kinesisSeqNumber: i.Id };
        })
      );
    }
  } catch (exc) {
    console.log("error sending message", exc);
    throw exc;
  }
  return error;
}
