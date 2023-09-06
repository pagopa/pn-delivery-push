const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  BatchExecuteStatementCommand,
} = require("@aws-sdk/lib-dynamodb");

const TABLES = {
  TIMELINES: "pn-Timelines",
  ACTION: "pn-Action",
  FUTUREACTION: "pn-FutureAction",
};

exports.TABLES = TABLES;

exports.persistEvents = async (events) => {
  const summary = {
    insertions: 0,
    errors: [],
  };

  const dynamoDB = DynamoDBDocumentClient.from(client);

  for (let persistEvent of events) {
    // ...
  }

  return summary;
};
