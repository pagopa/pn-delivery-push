const {
  parseISO,
  actTime,
  nextTimeSlot,
  toString: dateToString,
  isAfter,
} = require("./timeHelper");
const { generateKoResponse, generateOkResponse } = require("./responses");
const {
  getLastTimeSlotWorked,
  setLastTimeSlotWorked,
  getActionsByTimeSlot,
  batchDelete,
} = require("./dynamoFunctions.js");

const { BatchOperationException, InvalidItemException } = require("./exceptions.js");
const config = require("config");

const LAST_WORKED_KEY = config.get("LAST_WORKED_KEY");
const TOLLERANCE_IN_MILLIS = config.get("TOLLERANCE_IN_MILLIS");

const isTimeToLeave = (context) =>
  context.getRemainingTimeInMillis() < TOLLERANCE_IN_MILLIS;

async function handleEvent(event, context) {
  console.log("[FUTURE_ACTIONS_REMOVER]", "Started");

  let lastPollTable = config.get("LAST_POLL_TABLE_NAME");
  let futureActionTable = config.get("FUTURE_TABLE_NAME");

  let startTimeSlotStr;
  console.debug(
    "[FUTURE_ACTIONS_REMOVER]",
    "TABLES",
    lastPollTable,
    futureActionTable
  );
  try {
    startTimeSlotStr = await getLastTimeSlotWorked(
      lastPollTable,
      LAST_WORKED_KEY
    );
  } catch (e) {
    console.error(e);
    return generateKoResponse(e);
  }

  let startTimeSlot = parseISO(startTimeSlotStr);
  console.info(
    "[FUTURE_ACTIONS_REMOVER]",
    "STARTING FROM TIMESLOT",
    startTimeSlotStr,
    startTimeSlot
  );
  let endTimeSlot = actTime();
  console.info("[FUTURE_ACTIONS_REMOVER]", "ENDING TO  TIMESLOT", endTimeSlot);
  startTimeSlot = nextTimeSlot(startTimeSlot);
  console.debug("[FUTURE_ACTIONS_REMOVER]", "NEXT TIMESLOT", startTimeSlot);

  while (isAfter(endTimeSlot, startTimeSlot)) {
    let lastEvaluatedKey = undefined;

    const isTimingOut = (context) => () => isTimeToLeave(context);
    let haveDiscardedRecords = false;
    do {
      if (isTimeToLeave(context)) {
        console.info(
          "[FUTURE_ACTIONS_REMOVER]",
          "LEAVING FOR FUNCTION TIMEOUT"
        );
        return generateOkResponse(true);
      }

      console.debug(
        "[FUTURE_ACTIONS_REMOVER]",
        `Looking for items at timestlot ${startTimeSlot} with lastKey ${lastEvaluatedKey}`
      );


      let result = await getActionsByTimeSlot(
        futureActionTable,
        dateToString(startTimeSlot),
        lastEvaluatedKey
      );

      if (!result.items || result.items.length == 0) {
        console.debug(
          "[FUTURE_ACTIONS_REMOVER]",
          `No [more] items at timestlot ${startTimeSlot}`
        );
        break;
      }
      try {
        console.debug(
          "[FUTURE_ACTIONS_REMOVER]",
          `REMOVING ${result.items.length} items from ${startTimeSlot}`
        );

        const deleteResult = await batchDelete(
          futureActionTable,
          result.items,
          isTimingOut(context)
        );
        if (!deleteResult.operationResult) {
          console.warn(
            "[FUTURE_ACTIONS_REMOVER]",
            "Batch delete failure: operation aborted"
          );
          return generateOkResponse(false);
        }

        //do not override a override a true  value for discarded flag
        haveDiscardedRecords |= (deleteResult.discarded != 0);
        if (isTimeToLeave(context)) {
          console.info(
            "[FUTURE_ACTIONS_REMOVER]",
            "LEAVING FOR FUNCTION TIMEOUT"
          );
          return generateOkResponse(true);
        }
      } catch (e) {
        return generateKoResponse(new BatchOperationException("DELETE", e));
      }
      lastEvaluatedKey = result.lastEvaluatedKey;
      if (lastEvaluatedKey) {
        console.debug(
          "[FUTURE_ACTIONS_REMOVER]",
          "RESULTS LIMIT reached: paginating"
        );
      }
    } while (lastEvaluatedKey);
    
    // discarded records are present?
    if (haveDiscardedRecords) {
      console.warn(
        "[FUTURE_ACTIONS_REMOVER]",
        `Discarded record in timeslot  ${startTimeSlot}, exiting`
      );
      return generateKoResponse(new InvalidItemException());
    }
    else {
    setLastTimeSlotWorked(
      lastPollTable,
      LAST_WORKED_KEY,
      dateToString(startTimeSlot)
    );
  }
    console.log(
      "[FUTURE_ACTIONS_REMOVER]",
      `Last TimeSlot Worked  ${startTimeSlot}`
    );
    startTimeSlot = nextTimeSlot(startTimeSlot);
  }
  console.log("[FUTURE_ACTIONS_REMOVER]", "TIMESLOT PROCESSING FINISHED");

  return generateOkResponse(false);
}

module.exports = { handleEvent };
