const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { DynamoDBDocumentClient, UpdateCommand, GetCommand } = require("@aws-sdk/lib-dynamodb");

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
    throw new Error("iun, reworkId and status are mandatory");
  }
  if (item.timelineElementIds !== undefined && item.category === undefined) {
    throw new Error("timelineElementIds needs category to be set");
  }

  const Key = { iun: item.iun, reworkId: item.reworkId };

  const ExpressionAttributeNames = { "#s": "status", "#updatedAt": "updatedAt" };
  const ExpressionAttributeValues = {
    ":newStatus": item.status,
    ":updatedAt": new Date().toISOString(),
  };

  const setClauses = ["#s = :newStatus", "#updatedAt = :updatedAt"];

  const OPTIONAL_FIELDS = [
    "error",
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

const updateRequestRework = async (item, reworkEntity) => {
  if (!item?.iun || !item?.reworkId || !item?.status) {
    throw new Error("iun, reworkId e status are mandatory");
  }

  const now = new Date().toISOString();

  const okUpdateRequestEntry = {
    date: now,
    status: "OK",
    newStatusCodes: item.updateRequest.expectedStatusCodes,
    newDeliveryFailureCause: item.updateRequest.expectedDeliveryFailureCause,
    oldStatusCodes: reworkEntity.Item.expectedStatusCodes,
    oldDeliveryFailureCause: reworkEntity.Item.expectedDeliveryFailureCause
  };

  const koUpdateRequestEntry = {
    date: now,
    status: "KO",
    error: item.updateRequest.error,
    newStatusCodes: item.updateRequest.expectedStatusCodes,
    newDeliveryFailureCause: item.updateRequest.expectedDeliveryFailureCause
  };

  const params = {
    TableName: TABLE_NAME,
    Key: {
      iun: item.iun,
      reworkId: item.reworkId
    },
    ExpressionAttributeNames: {
      "#status": "status",
      "#updatedAt": "updatedAt",
      "#updateRequests": "updateRequests"
    },
    ExpressionAttributeValues: {
      ":ready": "READY",
      ":now": now,
      ":emptyList": []
    }
  };

  if (item.updateRequest.status === "OK") {
    params.ExpressionAttributeNames["#expectedStatusCodes"] = "expectedStatusCodes";
    params.ExpressionAttributeNames["#deliveryFailureCause"] = "deliveryFailureCause";
    params.ExpressionAttributeValues[":newExpectedStatusCodes"] = item.expectedStatusCodes;
    params.ExpressionAttributeValues[":newDeliveryFailureCause"] = item.deliveryFailureCause;
    params.ExpressionAttributeValues[":updateRequestOk"] = [okUpdateRequestEntry];
    params.UpdateExpression = `
          SET
            #status = :ready,
            #updatedAt = :now,
            #updateRequests = list_append(
              if_not_exists(#updateRequests, :emptyList),
              :updateRequestOk
            ),
            #expectedStatusCodes = :newExpectedStatusCodes,
            #deliveryFailureCause = :newDeliveryFailureCause
        `;
  } else if (item.updateRequest.status === "KO") {
    params.ExpressionAttributeValues[":updateRequestKo"] = [koUpdateRequestEntry];
    params.UpdateExpression = `
          SET
            #status = :ready,
            #updatedAt = :now,
            #updateRequests = list_append(
              if_not_exists(#updateRequests, :emptyList),
              :updateRequestKo
            )
        `;
  } else {
    throw new Error(`Not Supported status for UPDATE_REQUEST operation: ${item.updateRequest.status}`);
  }

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

const getReworkEntity = async (item) => {
  if (!item?.iun || !item?.reworkId) {
    throw new Error("iun, reworkId are mandatory");
  }

  const params = {
    TableName: TABLE_NAME,
    Key: {
      iun: item.iun,
      reworkId: item.reworkId
    }
  };

  return await docClient.send(new GetCommand(params));
};


module.exports = { updateRework, updateRequestRework, getReworkEntity, TABLE_NAME };