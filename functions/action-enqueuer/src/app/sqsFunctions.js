const { SendMessageBatchCommand, SQSClient } = require("@aws-sdk/client-sqs");
const { NodeHttpHandler } = require("@aws-sdk/node-http-handler");

const { v4 } = require("uuid");
const config = require("config");

const { SQSServiceException, TimeoutException } = require("./exceptions");

const MAX_SQS_BATCH = config.get("MAX_SQS_BATCH_SIZE");
const DEFAULT_SOCKET_TIMEOUT = config.get("timeout.DEFAULT_SOCKET_TIMEOUT");
const DEFAULT_REQUEST_TIMEOUT = config.get("timeout.DEFAULT_REQUEST_TIMEOUT");
const DEFAULT_CONNECTION_TIMEOUT = config.get(
  "timeout.DEFAULT_CONNECTION_TIMEOUT"
);
const TIMEOUT_EXCEPTIONS = config.get("TIMEOUT_EXCEPTIONS");

const defaultRequestHandler = new NodeHttpHandler({
  connectionTimeout: DEFAULT_CONNECTION_TIMEOUT,
  requestTimeout: DEFAULT_REQUEST_TIMEOUT,
  socketTimeout: DEFAULT_SOCKET_TIMEOUT,
});

function mapActionToQueueMessage(action) {
  let uuid = v4();
  let origAction = Object.assign({}, action);
  origAction.timeslot = action.timeSlot;
  delete origAction.timeSlot;
  delete origAction.seqNo;
  const message = {
    Id: uuid,
    DelaySeconds: 0,
    MessageAttributes: {
      createdAt: {
        DataType: "String",
        StringValue: new Date().toISOString(),
      },
      eventId: {
        DataType: "String",
        StringValue: uuid,
      },
      eventType: {
        DataType: "String",
        StringValue: "ACTION_GENERIC",
      },
      iun: {
        DataType: "String",
        StringValue: action.iun,
      },
      publisher: {
        DataType: "String",
        StringValue: "deliveryPush",
      },
    },
    MessageBody: JSON.stringify(origAction),
  };
  return message;
}

async function putMessages(sqsConfig, actions, isTimedOut) {
  let messagesToSend;

  while (actions.length > 0 && !isTimedOut()) {
    messagesToSend = [];
    let currentChunk = actions.splice(0, MAX_SQS_BATCH);
    currentChunk.forEach((action) => {
      let messageToSend = mapActionToQueueMessage(action);
      action.Id = messageToSend.Id;
      messagesToSend.push(messageToSend);
    });
    try {
      const failed = await _sendMessages(sqsConfig, messagesToSend);
      if (failed.length != 0) {
        // TODO
        console.error(
          "[ACTION_ENQUEUER]",
          "Aborting for an error on sending messages. Failed messages:",
          failed
        );
        currentChunk.forEach((element) => {
          if (
            failed.filter((currFailed) => currFailed.Id == element.Id)
              .length !== 0
          ) {
            delete element.Id;
            actions.push(element);
          }
        });

        return actions;
      }
    } catch (ex) {
      if (ex instanceof TimeoutException) {
        manageTimeout(sqsConfig, messagesToSend);
      }
      else {
        console.log(
          "[ACTION_ENQUEUER]",
          "Adding back the current chunk and aborting"
        );
        currentChunk.forEach((element) => {
          delete element.Id;
          actions.push(element);
        });
        console.log("[ACTION_ENQUEUER]", "Remaining actions:", actions);
        return actions;
      }
    }
  } //while
  if (actions.length !== 0)
    console.log(
      "[ACTION_ENQUEUER]",
      "Timeout reached. Remaining actions:",
      actions
    );
  return actions;
}

async function manageTimeout(sqsConfig, messagesToSend) {
  let timeoutConfig = Object.assign({}, sqsConfig);
  timeoutConfig.endpoint = sqsConfig.timeoutEndpoint;
  console.log("XXXXXXX", timeoutConfig);
  try {
    const failed = await _sendMessages(timeoutConfig, messagesToSend);

    if (failed.length != 0)
      console.error(
        "[ACTION_ENQUEUER]",
        "Can't write timeouted message on DLQ:",
        JSON.stringify(failed)
      );


  } catch (ex) {
    console.error(
      "[ACTION_ENQUEUER]",
      "Timeout detected for:",
      JSON.stringify(messagesToSend)
    );
  }
}

async function _sendMessages(sqsParams, messages) {
  try {
    if (!sqsParams || !sqsParams.endpoint)
      throw new Error("No SQS queue supplied");
    console.debug(
      "[ACTION_ENQUEUER]",
      `Sending a Batch of messages with following SQS parameters ${JSON.stringify(
        sqsParams
      )}`
    );
    if (!messages || messages.length === 0)
      throw new Error("No messages to send");

    console.debug(
      "[ACTION_ENQUEUER]",
      `Proceeding to send ${messages.length} messages to ${sqsParams.endpoint}`
    );

    //  chunking messages to send
    const input = {
      Entries: messages,
      QUEUE_URL: sqsParams.endpoint,
      //requestHandler: defaultRequestHandler,
    };

    console.log(
      "[ACTION_ENQUEUER]",
      "Sending the following batch of messages:",
      input
    );

    const command = new SendMessageBatchCommand(input);
    const sqs = new SQSClient({
      ...sqsParams,
      requestHandler: defaultRequestHandler,
    });
    const response = await sqs.send(command);
    console.debug("[ACTION_ENQUEUER]", "Sent message response", response);
    if (response.Failed && response.Failed.length > 0) {
      console.warn("[ACTION_ENQUEUER]", "Failed Messages", response.Failed);
      return response.Failed;
    }
  } catch (exc) {

    if (TIMEOUT_EXCEPTIONS.includes(exc.name)) {
      console.warn(
        "[ACTION_ENQUEUER]",
        "Timeout detected for:",
        JSON.stringify(messages)
      );
      throw new TimeoutException(exc);
    } else {
      console.error(
        "[ACTION_ENQUEUER]",
        "Error sending messages",
        JSON.stringify(messages),
        sqsParams,
        exc
      );
      throw new SQSServiceException(exc);
    }
  }
  return [];
}

module.exports = { putMessages };
