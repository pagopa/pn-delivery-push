const { DynamoDBDocument } = require("@aws-sdk/lib-dynamodb");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { isTimeToLeave, addDaysToDate } = require("../utils/utils.js");
const config = require("config");

const MAX_DYNAMO_BATCH = config.get("MAX_DYNAMO_BATCH");
const FUTURE_ACTION_TABLE_NAME = config.get("FUTURE_ACTION_TABLE_NAME");

const ddbClient = new DynamoDBClient();
const ddbDocClient = DynamoDBDocument.from(ddbClient, {
  marshallOptions: {
    removeUndefinedValues: true,
  }
});

async function writeMessagesToDynamo(arrayActionToStore, context) {
    while (arrayActionToStore.length > 0 && !isTimeToLeave(context)) {
      let splicedActionsArray = arrayActionToStore.splice(0, MAX_DYNAMO_BATCH); // vengono presi i primi N elementi rimuovendoli dall'array originale
  
      var futureActionItemMappedDynamoArray = [];
      splicedActionsArray.forEach(function (action) {

        let futureActionToDynamo = getFutureAction(action);
        console.log("futureAction to send is ", futureActionToDynamo);
        var futureActionItemDynamo = {
          PutRequest: {
            Item: futureActionToDynamo
          },
        };
        futureActionItemMappedDynamoArray.push(futureActionItemDynamo);
      });
  
      try {
        console.log("start to batchWrite items  ", futureActionItemMappedDynamoArray);

        var params = {
          RequestItems: {
            [FUTURE_ACTION_TABLE_NAME]: futureActionItemMappedDynamoArray,
          }
        };
        let response = await ddbDocClient.batchWrite(params);
        if (
          response.UnprocessedItems &&
          response.UnprocessedItems[FUTURE_ACTION_TABLE_NAME]
        ) {
          return handleUnprocessedItems(splicedActionsArray, response);
        }
      } catch (exceptions) {
        console.error("Dynamo cannot write items. Exception is", exceptions);
        return splicedActionsArray;
      }
    }
  
    console.log(
      "writeMessagesToDynamo completed. returned element is ",
      JSON.stringify(arrayActionToStore)
    );
    return arrayActionToStore;
}

function handleUnprocessedItems(splicedActionsArray, response) {
  let unprocessedItems = response.UnprocessedItems[FUTURE_ACTION_TABLE_NAME];
  let notPushedAction = [];

  console.log(
    "There are unprocessed items ",
    unprocessedItems
  );

  splicedActionsArray.forEach(function (splicedFutureAction) {
    if (unprocessedItems.filter( (unprocessedItem) => checkIsPresent(unprocessedItem, splicedFutureAction.actionId)).length != 0) {
      notPushedAction.push(splicedFutureAction);
    }
  });

  return notPushedAction;
}
function checkIsPresent(unprocessedItem, actionIdToFind){
  if(unprocessedItem.PutRequest && unprocessedItem.PutRequest.Item){
    return  unprocessedItem.PutRequest.Item.actionId == actionIdToFind ;
  }
  return false;
}

function getFutureAction(action){
  console.log('Starting get future action for ', JSON.stringify(action))
  const ttlTimeToAdd = config.get("FUTURE_ACTION_TTL_EXTRA_DAYS");
  let ttl = addDaysToDate(action.notBefore, ttlTimeToAdd);
  console.log('ttl calculated is ', ttl)

  let futureAction = {
    timeSlot: action.timeslot,
    actionId: action.actionId,
    notBefore: action.notBefore,
    recipientIndex: action.recipientIndex,
    type: action.type,
    timelineId: action.timelineId,
    iun: action.iun,
    details: getActionDetails(action.details),
    ttl: ttl
  };

  return futureAction;
}

function getActionDetails(actionDetails) {
    if (actionDetails) {
        return {
          quickAccessLinkToken: actionDetails.quickAccessLinkToken,
          key: actionDetails.key,
          documentCreationType: actionDetails.documentCreationType,
          timelineId: actionDetails.timelineId,
          retryAttempt: actionDetails.retryAttempt,
          startWorkflowTime: actionDetails.startWorkflowTime,
          errors: actionDetails.errors,
          isFirstSendRetry: actionDetails.isFirstSendRetry,
          alreadyPresentRelatedFeedbackTimelineId: actionDetails.alreadyPresentRelatedFeedbackTimelineId,
          lastAttemptAddressInfo: actionDetails.lastAttemptAddressInfo,
        };
    }
    return actionDetails;
}

module.exports = { writeMessagesToDynamo };