/* eslint-disable no-unused-vars */
const { expect, assert } = require("chai");
const { describe, it, before, after, afterEach } = require("mocha");
const sinon = require("sinon");
const proxyquire = require("proxyquire").noPreserveCache();

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
    const keyValue = 1;
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
    const keyValue = 1;

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
    const keyValue = 1;
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
    const keyValue = 1;

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
      KeyConditionExpression: "timeSlot = :ts",
      ExpressionAttributeValues: {
        ":ts": keyValue,
      }
    };

    const resultItems = [];

    ddbMock.on(QueryCommand, params).resolves({
      Items: [],
    });
    const result = await getActionsByTimeSlot(params.TableName, keyValue);
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
      KeyConditionExpression: "timeSlot = :ts",
      ExpressionAttributeValues: {
        ":ts": keyValue
      },
    };
    const resultItems = [{ iun: "iunTest" }];

    ddbMock.on(QueryCommand, params).resolves({
      Items: resultItems,
    });
    const result = await getActionsByTimeSlot(params.TableName, keyValue);

    expect(result).to.deep.equal({
      timeSlot: keyValue,
      items: resultItems,
      lastEvaluatedKey: undefined,
    });
  });

  it("test BatchDelete: item less than batch", async () => {
    let mockResponse = {
      UnprocessedItems: {},
    };

    // Stubbing DynamoDBClient and DynamoDBDocumentClient
    const DynamoDBClientStub = sinon.stub();
    const batchWriteStub = sinon.stub();

    // Assegna la funzione di stubbing al metodo batchWrite di DynamoDBDocumentClient
    DynamoDBClientStub.prototype.batchWrite = batchWriteStub;
    batchWriteStub.returns(mockResponse);
    let count = 0;
    const lambda = proxyquire.noCallThru().load("../app/dynamoFunctions.js", {
      "@aws-sdk/lib-dynamodb": {
        DynamoDBClient: DynamoDBClientStub,
        BatchWriteCommand: sinon.stub(),
        DynamoDBDocumentClient: {
          from: () => ({
            batchWrite: batchWriteStub,
            send: (params) => {
              count++;

              return mockResponse;
            },
          }),
        },
      },
    });

    const result = await lambda.batchDelete(
      "placeholder",
      require("./testData/deleteTest-lessBatchSize/dataset.json"),
      () => false
    );
    console.log("result", result);
    expect(count).to.be.equal(1);
    expect(result).to.not.be.undefined;
    expect(result.operationResult).to.be.true;
    expect(result.discarded).to.be.equal(0);
  });

  it("test BatchDelete: item more than  a batch", async () => {
    let mockResponse = {
      UnprocessedItems: {},
    };

    // Stubbing DynamoDBClient and DynamoDBDocumentClient
    const DynamoDBClientStub = sinon.stub();
    const batchWriteStub = sinon.stub();

    // Assegna la funzione di stubbing al metodo batchWrite di DynamoDBDocumentClient
    DynamoDBClientStub.prototype.batchWrite = batchWriteStub;
    batchWriteStub.returns(mockResponse);
    let count = 0;
    const lambda = proxyquire.noCallThru().load("../app/dynamoFunctions.js", {
      "@aws-sdk/lib-dynamodb": {
        DynamoDBClient: DynamoDBClientStub,
        BatchWriteCommand: sinon.stub(),
        DynamoDBDocumentClient: {
          from: () => ({
            batchWrite: batchWriteStub,
            send: (params) => {
              count++;

              return mockResponse;
            },
          }),
        },
      },
    });

    const result = await lambda.batchDelete(
      "placeholder",
      require("./testData/deleteTest-moreBatchSize/dataset.json"),
      () => false
    );

    expect(result).to.not.be.undefined;
    expect(result.operationResult).to.be.true;
    expect(result.discarded).to.be.equal(0);
  });

  it("test BatchDelete: batch operation Error", async () => {
    // Stubbing DynamoDBClient and DynamoDBDocumentClient

    const testData = require("./testData/deleteTest-moreBatchSize/dataset.json");

    let mockResponse = {
      UnprocessedItems: {
        "pn-FutureAction": [
          {
            DeleteRequest: {
              Item: testData[0],
            },
          },
        ],
      },
    };

    const DynamoDBClientStub = sinon.stub();
    const batchWriteStub = sinon.stub();

    // Assegna la funzione di stubbing al metodo batchWrite di DynamoDBDocumentClient
    DynamoDBClientStub.prototype.batchWrite = batchWriteStub;
    batchWriteStub.returns(mockResponse);
    let count = 0;
    const lambda = proxyquire.noCallThru().load("../app/dynamoFunctions.js", {
      "@aws-sdk/lib-dynamodb": {
        DynamoDBClient: DynamoDBClientStub,
        BatchWriteCommand: sinon.stub(),
        DynamoDBDocumentClient: {
          from: () => ({
            batchWrite: batchWriteStub,
            send: (params) => {
              count++;

              return mockResponse;
            },
          }),
        },
      },
    });

    let retryNo = 0;
    const MAX_RETRY_NO = 5;

    const isTimeToLeave = () => {
      retryNo++;
      if (retryNo < MAX_RETRY_NO) return false;

      return true;
    };

    const res = await lambda.batchDelete(
      "placeholder",
      testData,
      isTimeToLeave
    );

    //expect(res).to.be.false;
    expect(res).to.not.be.undefined;
    expect(res.operationResult).to.be.false;
    expect(res.discarded).to.be.equal(0);
  });
});
