const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, UpdateCommand } = require("@aws-sdk/lib-dynamodb");

// Nome tabella da env (obbligatorio)
const TABLE_NAME = process.env.NOTIFICATION_REWORKS_DYNAMO_TABLENAME;
if (!TABLE_NAME) {
  throw new Error("Missing env var: NOTIFICATION_REWORKS_DYNAMO_TABLENAME");
}

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client, {
  marshallOptions: { removeUndefinedValues: true },
});

const updateRework = async (item, expectedStates) => {
  if (!item || !item.iun || !item.reworkId || !item.status) {
    throw new Error("iun, reworkId e status sono obbligatori");
  }
  if (item.timelineElementIds !== undefined && item.category === undefined) {
    throw new Error("timelineElementIds richiede category");
  }

  const Key = { iun: item.iun, reworkId: item.reworkId };

  const ExpressionAttributeNames = { "#s": "status", "#updatedAt": "updatedAt" };
  const ExpressionAttributeValues = {
    ":newStatus": item.status,
    ":updatedAt": new Date().toISOString(),
  };

  const setClauses = ["#s = :newStatus", "#updatedAt = :updatedAt"];

  const OPTIONAL_FIELDS = [
    "errors",
    "category",
    "timelineElementIds"
  ];

  OPTIONAL_FIELDS.forEach((field) => {
    if (item[field] !== undefined) {
      const nameKey = `#${field}`;
      const valueKey = `:${field}`;
      ExpressionAttributeNames[nameKey] = field;
      ExpressionAttributeValues[valueKey] = item[field];
      setClauses.push(`${nameKey} = ${valueKey}`);
    }
  });

  const UpdateExpression = `SET ${setClauses.join(", ")}`;

  let ConditionExpression;
  const hasExpected = Array.isArray(expectedStates) && expectedStates.length > 0;
  if (item.status !== "ERROR" && hasExpected) {
    const placeholders = expectedStates.map((_, i) => `:state${i}`);
    ConditionExpression = `#s IN (${placeholders.join(", ")})`;
    expectedStates.forEach((st, i) => (ExpressionAttributeValues[`:state${i}`] = st));
  }

  const params = {
    TableName: TABLE_NAME,
    Key,
    UpdateExpression,
    ExpressionAttributeNames,
    ExpressionAttributeValues,
    ...(ConditionExpression && { ConditionExpression }),
  };

  try {
    await docClient.send(new UpdateCommand(params));
    return { ok: true };
  } catch (error) {
    if (error.name === "ConditionalCheckFailedException") {
      return { ok: false, reason: "CONDITION_FAILED" };
    }
    throw error;
  }
};

module.exports = { updateRework, TABLE_NAME };
