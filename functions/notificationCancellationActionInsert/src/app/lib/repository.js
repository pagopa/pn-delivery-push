const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  TransactWriteCommand,
} = require("@aws-sdk/lib-dynamodb");
const { nDaysFromNowAsUNIXTimestamp } = require("./utils");

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
  // if we put this here and not outside, we can change it at runtime
  // by changing the environment variable
  const ttlDays = process.env.ACTION_TTL_DAYS ?? 365; // not ||, because we want 0 to remain 0, not 365

  const dynamoDB = DynamoDBDocumentClient.from(client);

  for (let persistEvent of events) {
    const params = {
      TransactItems: [
        {
          Put: {
            TableName: TABLES.ACTION,
            Item: {
              // key
              actionId: persistEvent.actionId,
              // other attributes
              iun: persistEvent.iun, // GSI
              type: persistEvent.type,
              notBefore: persistEvent.notBefore,
              timeslot: persistEvent.timeslot,
              timelineId: persistEvent.timelineId,
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
              timeSlot: persistEvent.timeslot, // in this case in the table it's "timeSlot", not "timeslot"
              actionId: persistEvent.actionId,
              // others
              iun: persistEvent.iun, // GSI
              notBefore: persistEvent.notBefore,
              type: persistEvent.type,
            },
          },
        },
      ],
    };
    // ttl
    const ttl = nDaysFromNowAsUNIXTimestamp(ttlDays);
    if (ttl > 0) {
      params.TransactItems[0].Put.Item.ttl = ttl;
    }

    try {
      const result = await dynamoDB.send(new TransactWriteCommand(params));
      console.log("Action-FutureAction transaction succeeded:", result);
      summary.insertions++;
    } catch (error) {
      console.log(JSON.stringify(error));

      // check for ConditionalCheckFailed
      if (error.name == "TransactionCanceledException") {
        for (let cancellation of error.CancellationReasons) {
          if (cancellation.code == "ConditionalCheckFailed") {
            console.warn(
              "TransactionCanceledException: ConditionalCheckFailed"
            );
            // ignore this error, let the flow continue
          } else {
            console.error(
              "Error performing Action-FutureAction transaction:",
              error
            );
            summary.errors.push(persistEvent);
            break; // we want to return without processing other errors
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
