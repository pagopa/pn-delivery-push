const config = require("config");
const { SendMessageCommand, SQSClient } = require("@aws-sdk/client-sqs");
async function putMessage(sqsConfig, action) {
  const client = new SQSClient();
  const input = {
    QueueUrl: sqsConfig.endpoint,
    ...action,
  };
  const command = new SendMessageCommand(input);
  try {
    const response = await client.send(command);
    console.debug("[EVENTBRIDGE_TO_ACTION]", "SEND MESSAGE RESPONSE", response);
    return true;
  } catch (Error) {
    console.error("[EVENTBRIDGE_TO_ACTION]", Error);
    return false;
  }
}

async function handleEvent(event, context) {
  /*
  ricevuto evento  {
  version: '0',
  id: '77747fe0-faae-0b4f-6a79-e94a4cf2feff',
  'detail-type': 'PnDeliveryValidationOutcomeEvent',
  source: 'eventbridge.pn-delivery.insertTrigger',
  account: '830192246553',
  time: '2024-05-22T16:18:14Z',
  region: 'eu-central-1',
  resources: [],
  detail: {
    body: '{}',
    cxId: 'pn-delivery-push',
    MessageGroupId: 'DELIVERY-iunT-iunT-iunT-000000-T-0',
    MessageDeduplicationId: 'iunT-iunT-iunT-000000-T-0_start',
    MessageAttributes: {
      createdAt: [Object],
      eventId: [Object],
      eventType: [Object],
      iun: [Object],
      publisher: [Object]
    }
  }
}
  */
  let sqsParams = { endpoint: config.get("DESTINATION_ENDPOINT") };
  console.info("[EVENTBRIDGE_TO_ACTION]", "RECEIVED EVENT", event);
  if (!event.detail)
    return {
      statusCode: 500,
      body: JSON.stringify({ message: "no detail in event" }),
      isBase64Encoded: false,
    };
  const message = {
    Id: event.detail.Id,
    DelaySeconds: event.detail.DelaySeconds,
    MessageAttributes: event.detail.MessageAttributes,
    MessageBody: event.detail.body,
  };
  console.info("[EVENTBRIDGE_TO_ACTION]", "SENDING MESSAGE", message);
  const res = await putMessage(sqsParams, message);
  if (!res)
    return {
      statusCode: 500,
      isBase64Encoded: false,
    };

  return {
    statusCode: 200,
    isBase64Encoded: false,
    body: JSON.stringify({}),
  };
}

module.exports = { handleEvent };
