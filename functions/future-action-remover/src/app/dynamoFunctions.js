const {
  GetCommand,
  PutCommand,
  QueryCommand,
  DynamoDBDocumentClient,
  BatchWriteCommand,
} = require("@aws-sdk/lib-dynamodb");

const { ItemNotFoundException } = require("./exceptions.js");

const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { captureAWSv3Client } = require("aws-xray-sdk");

const { chunkIntoN } = require("./utils");

const ddbClient = captureAWSv3Client(new DynamoDBClient());

const ddbDocClient = DynamoDBDocumentClient.from(ddbClient);

const config = require("config");
const { isAfterStr } = require("./timeHelper.js");
const MAX_BATCH_SIZE = config.get("MAX_BATCH_SIZE");
const SLEEP_FOR_UNPROCESSED = config.get("SLEEP_FOR_UNPROCESSED");
async function getActionsByTimeSlot(
  tableName,
  { timeSlot, startTime, endTime },
  lastItem
) {
  const params = {
    TableName: tableName,
    KeyConditionExpression: "timeSlot = :ts",
    ExpressionAttributeValues: {
      ":ts": timeSlot,
      ":tS": startTime,
      ":tE": endTime,
    },
    FilterExpression: "notBefore >= :tS AND notBefore <= :tE",
  };

  console.debug(
    "[FUTURE_ACTIONS_REMOVER]",
    `Looking for data TS=${timeSlot}, START=${startTime}, END=${endTime}`
  );
  // starting from previous query result
  if (lastItem) {
    params.ExclusiveStartKey = lastItem;
  }
  const command = new QueryCommand(params);
  const result = await ddbDocClient.send(command);
  if (!result.Items || result.Items.length === 0) {
    console.warn(`No items found for timeSlot ${timeSlot}`);
  }

  return {
    timeSlot: timeSlot,
    items: result.Items,
    lastEvaluatedKey: result.LastEvaluatedKey,
  };
}

async function getLastTimeSlotWorked(tableName, keyValue) {
  const command = new GetCommand({
    TableName: tableName,
    Key: {
      lastPoolKey: +keyValue,
    },
  });
  const dynamoItem = await ddbDocClient.send(command);
  if (!dynamoItem || !dynamoItem.Item) {
    throw new ItemNotFoundException(keyValue, tableName);
  }

  return dynamoItem.Item.lastPollExecuted;
}

async function setLastTimeSlotWorked(tableName, keyValue, timeSlot) {
  const command = new PutCommand({
    TableName: tableName,
    Item: {
      lastPoolKey: +keyValue,
      lastPollExecuted: timeSlot,
    },
  });
  await ddbDocClient.send(command);
  //TODO CHECK RESPONSE
}

const _isToDiscard = (elem) => {
  return isAfterStr(elem.notBefore, elem.timeslot);
};

async function _wait(delay) {
  console.debug(
    "[FUTURE_ACTIONS_REMOVER]",
    `SLEEPING FOR ${delay} milliseconds`
  );
  return new Promise((resolve) => setTimeout(resolve, delay));
}

async function batchDelete(tableName, items, isTimingOut) {
  let chunkingResult = chunkIntoN(items, MAX_BATCH_SIZE, _isToDiscard);
  console.debug(
    "[FUTURE_ACTIONS_REMOVER]",
    `RECEIVED ${items.length} items to DELETE: will be done in  ${chunkingResult.chunks.length} chunks`
  );
  // For every chunk of MAX_BATCH_SIZE actions, make one BatchWrite request.
  for (const chunk of chunkingResult.chunks) {
    console.debug("[FUTURE_ACTIONS_REMOVER]", "DELETING  chunk", chunk);
    const deleteRequests = chunk.map((action) => ({
      DeleteRequest: {
        Key: { timeSlot: action.timeSlot, actionId: action.actionId },
      },
    }));

    let command = new BatchWriteCommand({
      RequestItems: {
        // An existing table is required. A composite key of 'title' and 'year' is recommended
        // to account for duplicate titles.
        [tableName]: deleteRequests,
      },
    });
    let doCycle = true;
    while (doCycle) {
      if (isTimingOut()) return { operationResult: false, discarded: chunkingResult.discarded};

      const res = await ddbDocClient.send(command);

      if (Object.keys(res.UnprocessedItems).length !== 0) {
        console.warn(
          "[FUTURE_ACTIONS_REMOVER]",
          `Unprocessed items in chunk: retry`,
          JSON.stringify(res.UnprocessedItems)
        );
        doCycle = true;
        command = new BatchWriteCommand({
          RequestItems: res.UnprocessedItems,
        });
        await _wait(SLEEP_FOR_UNPROCESSED);
      } else doCycle = false;
    }
  }
  return { operationResult: true, discarded: chunkingResult.discarded};
}

module.exports = {
  getActionsByTimeSlot,
  batchDelete,
  getLastTimeSlotWorked,
  setLastTimeSlotWorked,
};
