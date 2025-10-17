const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const {
  DynamoDBDocumentClient,
  UpdateCommand
} = require("@aws-sdk/lib-dynamodb");
const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client, {
  marshallOptions: { removeUndefinedValues: true },
});

const updateWithConditionalStatus = async (tablename, item, expectedStates) => {
  const statesArray = Array.isArray(expectedStates) ? expectedStates : [expectedStates];
  const placeholders = statesArray.map((_, index) => `:expectedState${index}`);
  const conditionExpression = `#s IN (${placeholders.join(', ')})`;

  const expressionAttributeValues = {};
  statesArray.forEach((state, index) => {
    expressionAttributeValues[`:expectedState${index}`] = state;
  });

  const key = {
    pk: item.iun,
    sk: item.reworkId,
  };

  let updateExpr = "SET #s = :newStatus, updatedAt = :updatedAt";
  expressionAttributeValues[":newStatus"] = item.status;
  expressionAttributeValues[":updatedAt"] = item.updatedAt;

  if (item.flagInvalidatedIds !== undefined) {
    updateExpr += ", invalidatedTimelineElementIds = :invalidatedIds";
    expressionAttributeValues[":invalidatedIds"] = item.flagInvalidatedIds;
  }

  const params = {
    TableName: tablename,
    Key: key,
    UpdateExpression: updateExpr,
    ConditionExpression: conditionExpression,
    ExpressionAttributeNames: {
      "#s": "status"
    },
    ExpressionAttributeValues: expressionAttributeValues,
  };

  try {
    const command = new UpdateCommand(params);
    await docClient.send(command);
    console.log(`updateItem eseguito con pk: ${item.iun} e reworkId: ${item.reworkId} su tabella: ${tablename}`);
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      console.log(`update non necessario per item con pk: ${item.iun} e reworkId: ${item.reworkId} su tabella: ${tablename}`);
    } else {
      console.log(`Errore ${error.message} durante updateWithConditionalStatus con pk: ${item.iun} e reworkId: ${item.reworkId} su tabella: ${tablename}`);
      throw error;
    }
  }
};

const updateStatusAndErrors = async (tablename, iun, reworkId, errors) => {
  const params = {
    TableName: tablename,
    Key: {
      pk: iun,
      sk: reworkId,
    },
    UpdateExpression: "SET #s = :error, #e = :errors",
    ExpressionAttributeNames: {
      "#s": "status",
      "#e": "errors"
    },
    ExpressionAttributeValues: {
      ":error": "ERROR",
      ":errors": errors
    }
  };
  try {
    const command = new UpdateCommand(params);
    await docClient.send(command);
    console.log(`update Status and Errors for item with pk: ${iun} and reworkId: ${reworkId} on table: ${tablename}`);
  } catch (error) {
    console.log(`Error ${error.message} during updateStatusAndErrors for item with pk: ${iun} and reworkId: ${reworkId} on table: ${tablename}`);
    throw error;
  }
};

module.exports = { updateWithConditionalStatus, updateStatusAndErrors };