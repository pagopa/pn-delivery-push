const { expect } = require("chai");
const {
  parseKinesisObjToJsonObj,
} = require("../app/lib/utils");

describe("test utils functions", () => {

  it("should parse kinesis obj", () => {
    const kinesisObj = {
      iun: {
        S: "abcd",
      },
      timelineElementId: {
        S: "notification_viewed_creation_request;IUN_XLDW-MQYJ-WUKA-202302-A-1;RECINDEX_1",
      },
      notificationSentAt: {
        S: "2023-01-20T14:48:00.000Z",
      },
      timestamp: {
        S: "2023-01-20T14:48:00.000Z",
      },
      paId: {
        S: "026e8c72-7944-4dcd-8668-f596447fec6d",
      },
      details: {
        M: {
          notificationCost: {
            N: 100,
          },
          recIndex: {
            N: 0,
          },
          aarKey: {
            S: "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG",
          },
        },
      },
    };
    const parsedObj = parseKinesisObjToJsonObj(kinesisObj);
    expect(parsedObj).to.eql({
      iun: "abcd",
      timelineElementId:
        "notification_viewed_creation_request;IUN_XLDW-MQYJ-WUKA-202302-A-1;RECINDEX_1",
      notificationSentAt: "2023-01-20T14:48:00.000Z",
      timestamp: "2023-01-20T14:48:00.000Z",
      paId: "026e8c72-7944-4dcd-8668-f596447fec6d",
      details: {
        notificationCost: 100,
        recIndex: 0,
        aarKey: "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG",
      },
    });
  });

  it("no kinesis obj", () => {
    const parsedObj = parseKinesisObjToJsonObj(null);
    expect(parsedObj).equal(null);
  });
});
