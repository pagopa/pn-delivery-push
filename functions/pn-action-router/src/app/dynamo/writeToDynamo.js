const { DynamoDBDocument } = require("@aws-sdk/lib-dynamodb");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { isTimeToLeave } = require("../utils/utils.js");
const config = require("config");

const MAX_DYNAMO_BATCH = config.get("MAX_DYNAMO_BATCH");
const FUTURE_ACTION_TABLE_NAME = config.get("FUTURE_ACTION_TABLE_NAME");

const ddbClient = new DynamoDBClient();
const ddbDocClient = DynamoDBDocument.from(ddbClient, {
  marshallOptions: {
    removeUndefinedValues: true,
  },
});

async function writeMessagesToDynamo(arrayActionToStore, context) {
    while (arrayActionToStore.length > 0 && !isTimeToLeave(context)) {
      let splicedActionsArray = arrayActionToStore.splice(0, MAX_DYNAMO_BATCH); // vengono presi i primi N elementi rimuovendoli dall'array originale
  
      var futureActionItemMappedDynamoArray = [];
      splicedActionsArray.forEach(function (action) {

        let futureActionToDynamo = getFutureAction(action);
        console.log("futureAction is ", futureActionToDynamo);
        var futureActionItemDynamo = {
          PutRequest: {
            Item: futureActionToDynamo
          },
        };
        futureActionItemMappedDynamoArray.push(futureActionItemDynamo);
      });
  
      try {
        console.log("start to batchWrite items  ", futureActionItemMappedDynamoArray);

        let retryCount = 0;
        var params = {
          RequestItems: {
            [FUTURE_ACTION_TABLE_NAME]: futureActionItemMappedDynamoArray,
          }
        };
  
        await handleBatchWriteItems(params, retryCount, context);

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

async function handleBatchWriteItems(params, retryCount, context){
  console.log('send batchWriteItem with params', JSON.stringify(params) )

  if(!isTimeToLeave(context)){
    let res = await ddbDocClient.batchWrite(params);
    console.log("Completed batchWrite. Response received from dynamo is ", res);
  
    if (Object.keys(res.UnprocessedItems).length !== 0) {
      console.error(
        "[PN_ACTION_ROUTER]",
        `Unprocessed items in chunk: retry`,
        JSON.stringify(res.UnprocessedItems)
      );
      params = {
        RequestItems: res.UnprocessedItems
      };
      
      retryCount = retryCount + 1;
      let waitMsTime = 2 ** retryCount * 10;
      console.log('waitMsTime ', waitMsTime);
      await wait(waitMsTime);
      console.log('sono dopo il wait')
      await handleBatchWriteItems(params, retryCount, context); 
    }
  
  }else{
    throw new Error('lambda execution time is close to expire');
  }
}

async function wait(delay) {
  return new Promise(resolve => setTimeout(resolve, delay));
}

function getFutureAction(action){
  let futureAction = {
    timeSlot: action.timeslot,
    actionId: action.actionId,
    notBefore: action.notBefore,
    recipientIndex: action.recipientIndex,
    type: action.type,
    timelineId: action.timelineId,
    iun: action.iun,
    details: getActionDetails(action.details)
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
        alreadyPresentRelatedFeedbackTimelineId:
            actionDetails.alreadyPresentRelatedFeedbackTimelineId,
        lastAttemptAddressInfo: actionDetails.lastAttemptAddressInfo,
        };
    }
    return actionDetails;
}

module.exports = { writeMessagesToDynamo };