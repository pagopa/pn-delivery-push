const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  TransactWriteCommand,
} = require("@aws-sdk/lib-dynamodb");

const TABLES = {
  TIMELINES: "pn-Timelines",
  ACTION: "pn-Action",
  FUTUREACTION: "pn-FutureAction",
};

const client = new DynamoDBClient({
  region: process.env.REGION,
});

exports.TABLES = TABLES;

exports.persistEvents = async (events) => {
  const summary = {
    insertions: 0,
    errors: [],
  };

  const dynamoDB = DynamoDBDocumentClient.from(client);

  for (let persistEvent of events) {
    const params = {
      TransactItems: [
        {
          Put: {
            TableName: TABLES.ACTION,
            Item: {
              // key
              actionId: { S: persistEvent.actionId },
              // other attributes
              iun: { S: persistEvent.iun }, // GSI
              type: { S: persistEvent.type },
              notBefore: { S: persistEvent.notBefore },
              timeslot: { S: persistEvent.timeslot },
              timelineId: { S: persistEvent.timelineId },
            },
            // condition for put if absent
            ConditionExpression: "attribute_not_exists(actionId)",
          },
        },
        {
          Put: {
            TableName: TABLES.FUTUREACTION,
            Item: {
              // key (composite)
              timeSlot: { S: persistEvent.timeslot }, // in this case "timeSlot", not "timeslot"
              actionId: { S: persistEvent.actionId },
              // others
              iun: { S: persistEvent.iun }, // GSI
              notBefore: { S: persistEvent.notBefore },
              type: { S: persistEvent.type },
            },
          },
        },
      ],
    };

    try {
      const result = await dynamoDB.send(new TransactWriteCommand(params));
      console.log("Action-FutureAction transaction succeeded:", result);
      summary.insertions++;
    } catch (error) {
      // check for ConditionalCheckFailed
      if (error.name == "TransactionCanceledException") {
        for (let cancellation of error.cancellationReasons) {
          if (cancellation.code == "ConditionalCheckFailed") {
            console.warn(
              "TransactionCanceledException: ConditionalCheckFailed"
            );
            // ignore this error
          } else {
            console.error(
              "Error performing Action-FutureAction transaction:",
              error
            );
            summary.errors.push(persistEvent);
          }
        }
      } else {
        console.error(
          "Error performing Action-FutureAction transaction:",
          error
        );
        summary.errors.push(persistEvent);
      }
    }
  }

  return summary;
};
