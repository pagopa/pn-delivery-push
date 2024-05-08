const { expect, assert } = require("chai");

const {
  DynamoDBDocumentClient,
  GetCommand,
  PutCommand,
  QueryCommand,
} = require("@aws-sdk/lib-dynamodb");

const { mockClient } = require("aws-sdk-client-mock");

const { DateTime } = require("luxon");

const {
  getActionsByTimeSlotTable,
  getLastTimeSlotWorkedTable,
  setLastTimeSlotWorkedTable,
  DEFAULT_FUTURE_TABLE_NAME,
  DEFAULT_LAST_POLL_TABLE_NAME,
} = require("../app/dynamoFunctions.js").default;

function errorMessageDynamo(id, table) {
  return "Item with id = " + id + " not found on table " + table;
}

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

  it("test getLastTimeSlotWorkedTable: item found", async () => {
    const keyValue = "1";
    const timeValue = "12345";

    const params = {
      TableName: DEFAULT_LAST_POLL_TABLE_NAME,
      Key: {
        lastPoolKey: keyValue,
      },
    };

    let result = { lastPoolKey: keyValue, lastPollExecuted: timeValue };
    ddbMock.on(GetCommand, params).resolves({
      Item: { lastPoolKey: keyValue, lastPollExecuted: timeValue },
    });

    const item = await getLastTimeSlotWorkedTable(params.TableName, keyValue);

    expect(item).to.be.equal(timeValue);
  });

  it("test getLastTimeSlotWorkedTable: item not found", async () => {
    const keyValue = "1";

    const params = {
      TableName: DEFAULT_LAST_POLL_TABLE_NAME,
      Key: {
        lastPoolKey: keyValue,
      },
    };
    ddbMock.on(GetCommand, params).resolves({ Item: null });

    try {
      await getLastTimeSlotWorkedTable(params.TableName, keyValue);
    } catch (error) {
      expect(error.name).to.be.eq("ItemNotFoundException");
      return;
    }

    assert(false);
  });

  it("test getLastTimeSlotWorkedTable: item found but wrong format", async () => {
    const keyValue = "1";
    const timeValue = "12345";

    const params = {
      TableName: DEFAULT_LAST_POLL_TABLE_NAME,
      Key: {
        lastPoolKey: keyValue,
      },
    };

    let result = { lastPoolKey: keyValue, lastPollExecuted: timeValue };
    ddbMock.on(GetCommand, params).resolves({
      Item: { lastPoolKey: keyValue, lastPollExecuted: timeValue },
    });

    const timeStr = await getLastTimeSlotWorkedTable(
      params.TableName,
      keyValue
    );

    const timeConverted = DateTime.fromISO(timeStr, { zone: "UTC" });
    expect(timeConverted.isValid).to.be.false;
  });

  it("test setLastTimeSlotWorkedTable: add item", async () => {
    const keyValue = "1";

    const params = {
      TableName: DEFAULT_LAST_POLL_TABLE_NAME,
      Key: {
        lastPoolKey: keyValue,
      },
    };

    ddbMock.on(PutCommand, params).resolves();

    const timeStr = await setLastTimeSlotWorkedTable(
      params.TableName,
      keyValue,
      DateTime.local()
        .toUTC()
        .toISO({ suppressMilliseconds: true, includeOffset: true })
    );
  });

  // it("test getActionsByTimeSlotTable not found", async () => {
  //   const id = "fake";
  //   const params = {
  //     TableName: "pn-paAggregations",
  //     Key: { ["x-pagopa-pn-cx-id"]: id },
  //   };
  //   ddbMock.on(GetCommand, params).resolves({ Item: null });
  //   try {
  //     await getActionsByTimeSlotTable(tableName, timeSlot, lastItem);
  //   } catch (error) {
  //     expect(error).to.not.be.null;
  //     expect(error).to.not.be.undefined;
  //     expect(error.message).to.equal(errorMessageDynamo(id, params.TableName));
  //   }
  // });

  // it("test getActionsByTimeSlotTable found", async () => {
  //   const id = "fake";
  //   const params = {
  //     TableName: "pn-paAggregations",
  //     Key: { ["x-pagopa-pn-cx-id"]: id },
  //   };
  //   ddbMock.on(GetCommand, params).resolves({ Item: null });
  //   try {
  //     await getActionsByTimeSlotTable(tableName, timeSlot, lastItem);
  //   } catch (error) {
  //     expect(error).to.not.be.null;
  //     expect(error).to.not.be.undefined;
  //     expect(error.message).to.equal(errorMessageDynamo(id, params.TableName));
  //   }
  // });

  // it("test getPaAggregateById found", async () => {
  //   const id = "test";
  //   const params = {
  //     TableName: "pn-aggregates",
  //     Key: { ["aggregateId"]: id },
  //   };
  //   ddbMock.on(GetCommand, params).resolves({
  //     Item: mockAggregateFound,
  //   });
  //   const item = await getPaAggregateById(id);
  //   expect(item.AWSApiKey).equal(mockAggregateFound.AWSApiKey);
  // });

  // it("test getPaAggregateById not found", async () => {
  //   const id = "fake";
  //   const params = {
  //     TableName: "pn-aggregates",
  //     Key: { ["aggregateId"]: id },
  //   };
  //   ddbMock.on(GetCommand, params).resolves({ Item: null });
  //   try {
  //     await getPaAggregateById(params.Key["aggregateId"]);
  //   } catch (error) {
  //     expect(error).to.not.be.null;
  //     expect(error).to.not.be.undefined;
  //     expect(error.message).to.equal(errorMessageDynamo(id, params.TableName));
  //   }
  // });

  // it("test getApiKeyByIndex found", async () => {
  //   ddbMock.on(QueryCommand).resolves({
  //     Items: [mockVirtualKey],
  //   });
  //   const item = await getApiKeyByIndex("test");
  //   expect(item.virtualKey).equal(mockVirtualKey.virtualKey);
  // });

  // it("test getApiKeyByIndex not found", async () => {
  //   ddbMock.on(QueryCommand).resolves({ Items: null });
  //   try {
  //     await getApiKeyByIndex("fakekey");
  //   } catch (error) {
  //     expect(error).to.not.be.null;
  //     expect(error).to.not.be.undefined;
  //     expect(error.message).to.equal(
  //       errorMessageDynamo("fa***ey", "pn-apiKey")
  //     );
  //   }
  // });

  // //Casistica impossibile
  // it("test getApiKeyByIndex too many items", async () => {
  //   ddbMock.on(QueryCommand).resolves({
  //     Items: [mockVirtualKey, mockVirtualKey],
  //   });
  //   try {
  //     await getApiKeyByIndex("test");
  //   } catch (error) {
  //     expect(error).to.not.be.null;
  //     expect(error).to.not.be.undefined;
  //     expect(error.message).to.equal("Too many items found on table pn-apiKey");
  //   }
  // });
});
