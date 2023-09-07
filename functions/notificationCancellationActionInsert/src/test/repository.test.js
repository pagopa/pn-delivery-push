const { expect } = require("chai");
const { persistEvents, ttlDays } = require("../app/lib/repository.js");
const { nDaysFromNowAsUNIXTimestamp } = require("../app/lib/utils.js");
const { mockClient } = require("aws-sdk-client-mock");
const {
  DynamoDBDocumentClient,
  TransactWriteCommand,
} = require("@aws-sdk/lib-dynamodb");

const ddbMock = mockClient(DynamoDBDocumentClient);

describe("DynamoDB tests", function () {
  this.beforeEach(() => {
    ddbMock.reset();
  });

  const events = [
    {
      actionId: "notification_cancellation_iun_XLDW-MQYJ-WUKA-202302-A-1",
      timeslot: "2021-09-23T10:00",
      iun: "XLDW-MQYJ-WUKA-202302-A-1",
      type: "NOTIFICATION_CANCELLATION",
      notBefore: "2021-09-23T10:00:00.000Z",
      timelineId:
        "notification_cancellation_request.IUN_XLDW-MQYJ-WUKA-202302-A-1",
      opType: "INSERT_ACTION_FUTUREACTION",
      kinesisSeqNumber: "4950",
    },
  ];

  it("test persistEvents", async () => {
    ddbMock.on(TransactWriteCommand).resolves({
      UnprocessedItems: {},
    });

    const res = await persistEvents(events);

    expect(res.insertions).equal(1);
    expect(res.errors.length).equal(0);
  });

  it("test persistEvents with ttl", async () => {
    ddbMock.on(TransactWriteCommand).resolves({
      UnprocessedItems: {},
    });

    const res = await persistEvents(events);

    expect(res.insertions).equal(1);
    expect(res.errors.length).equal(0);

    // check ttl
    const ttl = nDaysFromNowAsUNIXTimestamp(ttlDays);
    expect(
      ddbMock.calls()[0].args[0].input.TransactItems[0].Put.Item.ttl.N
    ).equal(ttl.toString());
  });

  // it("test persistEvents with ttl === 0", async () => {
  //   ddbMock.on(TransactWriteCommand).resolves({
  //     UnprocessedItems: {},
  //   });

  //   // mock, inside persistEvents, the ttlDays function, to return 0
  //   //const ttlDaysMock = sinon.stub();
  //   //ttlDaysMock.returns(0);
  //   const proxyquire = require("proxyquire");
  //   const persistEvents = proxyquire
  //     .noCallThru()
  //     .load("../app/lib/repository.js", {
  //       "./utils.js": {
  //         ttlDays: 0,
  //       },
  //     }).persistEvents;

  //   const res = await persistEvents(events);

  //   expect(res.insertions).equal(1);
  //   expect(res.errors.length).equal(0);

  //   expect(ddbMock.calls()[0].args[0].input.TransactItems[0].Put.Item.ttl).to.be
  //     .undefined;
  // });

  it("test persistEvents with ConditionalCheckFailed", async () => {
    ddbMock.on(TransactWriteCommand).rejects({
      name: "TransactionCanceledException",
      cancellationReasons: [
        {
          code: "ConditionalCheckFailed",
        },
      ],
    });

    const res = await persistEvents(events);

    expect(res.insertions).equal(0);
    expect(res.errors.length).equal(0);
  });

  it("test persistEvents with TransactionCanceledException but not ConditionalCheckFailed", async () => {
    ddbMock.on(TransactWriteCommand).rejects({
      name: "TransactionCanceledException",
      cancellationReasons: [
        {
          code: "Other",
        },
      ],
    });

    const res = await persistEvents(events);

    expect(res.insertions).equal(0);
    expect(res.errors.length).equal(1);
  });

  it("test persistEvents with a generic error", async () => {
    ddbMock.on(TransactWriteCommand).rejects({
      name: "OtherException",
    });

    const res = await persistEvents(events);

    expect(res.insertions).equal(0);
    expect(res.errors.length).equal(1);
  });
});
