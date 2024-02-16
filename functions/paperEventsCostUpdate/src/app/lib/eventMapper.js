const { parseKinesisObjToJsonObj } = require("./utils");
const crypto = require("crypto");

const TABLES = {
  TIMELINES: "pn-Timelines",
};

const allowedTimelineCategories = [
  "SEND_SIMPLE_REGISTERED_LETTER",
  "SEND_ANALOG_DOMICILE",
];

function updateCostPhaseForSendAnalogDomicile(timelineObj) {
  // timelineObj.timelineElementId must exist and be a string
  if (
    !timelineObj.timelineElementId ||
    typeof timelineObj.timelineElementId !== "string"
  ) {
    return null;
  }

  if (timelineObj.timelineElementId.indexOf("ATTEMPT_0") >= 0) {
    return "SEND_ANALOG_DOMICILE_ATTEMPT_0";
  } else if (timelineObj.timelineElementId.indexOf("ATTEMPT_1") >= 0) {
    return "SEND_ANALOG_DOMICILE_ATTEMPT_1";
  } else {
    return null;
  }
}

exports.mapEvents = async (events) => {
  const filteredEvents = events.filter((e) => {
    // INSERT events from TIMELINES table, with the allowed categories
    return (
      e.eventName == "INSERT" &&
      e.tableName == TABLES.TIMELINES &&
      e.dynamodb.NewImage.category &&
      allowedTimelineCategories.indexOf(e.dynamodb.NewImage.category.S) >= 0
    );
  });

  let result = [];

  for (const filteredEvent of filteredEvents) {
    const date = new Date();

    const timelineObj = parseKinesisObjToJsonObj(
      filteredEvent.dynamodb.NewImage
    );

    const resultElementBody = {
      // common to all events
      iun: timelineObj.iun,
      // common to all handled paper events
      recIndex: timelineObj.details?.recIndex ?? undefined,
      notificationStepCost: timelineObj.details?.analogCost ?? undefined,
      eventTimestamp: timelineObj.timestamp,
      eventStorageTimestamp: timelineObj.timestamp,
    };

    let messageAttributes = {
      publisher: {
        DataType: "String",
        StringValue: "deliveryPush",
      },
      iun: {
        DataType: "String",
        StringValue: resultElementBody.iun,
      },
      eventId: {
        DataType: "String",
        StringValue: crypto.randomUUID(),
      },
      createdAt: {
        DataType: "String",
        StringValue: date.toISOString(),
      },
      eventType: {
        DataType: "String",
        StringValue: "UPDATE_COST_PHASE_EVENT",
      },
    };

    const category = timelineObj.category;

    switch (category) {
      case "SEND_SIMPLE_REGISTERED_LETTER":
        resultElementBody.updateCostPhase = "SEND_SIMPLE_REGISTERED_LETTER";
        resultElementBody.vat = timelineObj.details?.vat ?? undefined;
        break;

      case "SEND_ANALOG_DOMICILE":
        const costPhase = updateCostPhaseForSendAnalogDomicile(timelineObj);
        if (costPhase) {
          resultElementBody.updateCostPhase = costPhase;
          resultElementBody.vat = timelineObj.details?.vat ?? undefined;
        }
        break;

      //default:
      // we previously filtered out the events with not allowed categories
      //  break;
    }

    let resultElement = {
      Id: filteredEvent.kinesisSeqNumber,
      MessageBody: JSON.stringify(resultElementBody),
      MessageAttributes: messageAttributes,
    };

    // we want to send the element to the queue only if fields are valid,
    // without sending them back to the stream, or we'd end up in an loop
    if (
      resultElementBody.updateCostPhase &&
      resultElementBody.recIndex &&
      resultElementBody.notificationStepCost
    ) {
      console.log(
        "Mapped message for the queue: %j",
        JSON.stringify(resultElement)
      );

      result.push(resultElement);
    } else {
      console.error(
        "Error in parsing timelineObj (analogCost or recIndex missing or incomplete timelineElementId): ",
        timelineObj
      );
    }
  }
  return result;
};
