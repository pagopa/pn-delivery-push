const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  TransactWriteItemsCommand,
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
              iun: { S: "exampleIun" },
              id: { S: "exampleId1" },
              user: { S: "exampleUser" },
              // ...
            },
            // condition for put if absent
            ConditionExpression:
              "attribute_not_exists(iun) AND attribute_not_exists(id)",
          },
        },
        {
          Put: {
            TableName: TABLES.FUTUREACTION,
            Item: {
              id: { S: "exampleId2" },
              // ...
            },
          },
        },
      ],
    };

    try {
      const result = await dynamoDB.send(new TransactWriteItemsCommand(params));
      console.log("Action-FutureAction transaction succeeded:", result);
    } catch (error) {
      console.error("Error performing Action-FutureAction transaction:", error);
      summary.errors.push(persistEvent);
    }
  }

  return summary;
};
