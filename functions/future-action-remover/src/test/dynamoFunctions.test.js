const { expect, assert } = require("chai");
const { describe, it, before, after, afterEach } = require("mocha");
const {
  DynamoDBDocumentClient,
  GetCommand,
  PutCommand,
  QueryCommand,
} = require("@aws-sdk/lib-dynamodb");

const { mockClient } = require("aws-sdk-client-mock");

const { DateTime } = require("luxon");

const {
  getLastTimeSlotWorked,
  setLastTimeSlotWorked,
  getActionsByTimeSlot,
  batchDelete,
} = require("../app/dynamoFunctions.js");

const config = require("config");
const LAST_POLL_TABLE_NAME = config.get("LAST_POLL_TABLE_NAME");
const FUTURE_TABLE_NAME = config.get("FUTURE_TABLE_NAME");

describe("dynamoFunctions tests", function () {
  let ddbMock;

  before(() => {
    ddbMock = mockClient(DynamoDBDocumentClient);
  });

  afterEach(() => {
    ddbMock.reset();
  });

  after(() => {
    ddbMock.restore();
  });

  it("test getLastTimeSlotWorked: item found", async () => {
    const keyValue = "1";
    const timeValue = "12345";

    const params = {
      TableName: LAST_POLL_TABLE_NAME,
      Key: {
        lastPoolKey: keyValue,
      },
    };

    ddbMock.on(GetCommand, params).resolves({
      Item: { lastPoolKey: keyValue, lastPollExecuted: timeValue },
    });

    const item = await getLastTimeSlotWorked(params.TableName, keyValue);

    expect(item).to.be.equal(timeValue);
  });

  it("test getLastTimeSlotWorked: item not found", async () => {
    const keyValue = "1";

    const params = {
      TableName: LAST_POLL_TABLE_NAME,
      Key: {
        lastPoolKey: keyValue,
      },
    };
    ddbMock.on(GetCommand, params).resolves({ Item: null });

    try {
      await getLastTimeSlotWorked(params.TableName, keyValue);
    } catch (error) {
      expect(error.name).to.be.eq("ItemNotFoundException");
      return;
    }

    assert(false);
  });

  it("test getLastTimeSlotWorked: item found but wrong format", async () => {
    const keyValue = "1";
    const timeValue = "12345";

    const params = {
      TableName: LAST_POLL_TABLE_NAME,
      Key: {
        lastPoolKey: keyValue,
      },
    };

    ddbMock.on(GetCommand, params).resolves({
      Item: { lastPoolKey: keyValue, lastPollExecuted: timeValue },
    });

    const item = await getLastTimeSlotWorked(params.TableName, keyValue);

    expect(item).to.be.equal(timeValue);
  });

  it("test setLastTimeSlotWorked: add item", async () => {
    const keyValue = "1";

    const params = {
      TableName: LAST_POLL_TABLE_NAME,
      Key: {
        lastPoolKey: keyValue,
      },
    };

    ddbMock.on(PutCommand, params).resolves();
    await setLastTimeSlotWorked(
      params.TableName,
      keyValue,
      DateTime.local()
        .toUTC()
        .toISO({ suppressMilliseconds: true, includeOffset: true })
    );
  });

  it("test getActionsByTimeSlot not found", async () => {
    const keyValue = "2024-05-20T12:00";
    const params = {
      TableName: FUTURE_TABLE_NAME,
      KeyConditionExpression:
        "timeSlot = :ts and notBefore >= :tS and notBefore <= :tE",
      ExpressionAttributeValues: {
        ":ts": keyValue,
        ":tS": keyValue,
        ":tE": keyValue,
      },
    };

    const resultItems = [];

    ddbMock.on(QueryCommand, params).resolves({
      Items: [],
    });
    const result = await getActionsByTimeSlot(params.TableName, {
      timeSlot: keyValue,
      startTime: keyValue,
      endTime: keyValue,
    });
    expect(result).to.deep.equal({
      timeSlot: keyValue,
      items: resultItems,
      lastEvaluatedKey: undefined,
    });
  });

  it("test getActionsByTimeSlot not found", async () => {
    const keyValue = "2024-05-20T12:00";
    const params = {
      TableName: FUTURE_TABLE_NAME,
      KeyConditionExpression:
        "timeSlot = :ts and notBefore >= :tS and notBefore <= :tE",
      ExpressionAttributeValues: {
        ":ts": keyValue,
        ":tS": keyValue,
        ":tE": keyValue,
      },
    };
    const resultItems = [{ iun: "iunTest" }];

    ddbMock.on(QueryCommand, params).resolves({
      Items: resultItems,
    });
    const result = await getActionsByTimeSlot(params.TableName, {
      timeSlot: keyValue,
      startTime: keyValue,
      endTime: keyValue,
    });

    expect(result).to.deep.equal({
      timeSlot: keyValue,
      items: resultItems,
      lastEvaluatedKey: undefined,
    });
  });

  it("test BatchDelete: item less than batch", async () => {});

  it("test BatchDelete: item more than  a batch", async () => {});
});
