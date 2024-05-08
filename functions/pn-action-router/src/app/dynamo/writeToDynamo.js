const { DynamoDBDocument } = require("@aws-sdk/lib-dynamodb");
const { DynamoDBClient } = require("@aws-sdk/client-dynamodb");
const { isTimeToLeave } = require("../utils/utils.js");

const ddbClient = new DynamoDBClient();

const ddbDocClient = DynamoDBDocument.from(ddbClient, {
  marshallOptions: {
    removeUndefinedValues: true,
  },
});

async function writeMessagesToDynamo(futureActions, context) {
    while (futureActions.length > 0 && !isTimeToLeave(context)) {
      let splicedFutureActionsArray = futureActions.splice(0, 1); // prendo i primi 10 e rimuovendoli dall'array originale
  
      var actionItemMappedDynamoArray = [];
      splicedFutureActionsArray.forEach(function (action) {
        var date = new Date();
        let isoDateNow = date.toISOString();
    
        let futureAction = {
          timeSlot: action.timeslot,
          actionId: action.actionId,
          notBefore: action.notBefore,
          recipientIndex: action.recipientIndex,
          type: action.type,
          timelineId: action.timelineId,
          iun: action.iun,
          details: getActionDetails(action.details),
          insertActionTimestamp: action.insertActionTimestamp,
          insertFutureActionTimestamp: isoDateNow
        };
        console.log("futureAction is ", futureAction);
  
        var actionItemDynamo = {
          PutRequest: {
            Item: futureAction,
          },
        };
  
        actionItemMappedDynamoArray.push(actionItemDynamo);
      });
  
      var params = {
        RequestItems: {
          PocFutureAction: actionItemMappedDynamoArray,
        },
      };
  
      try {
        console.log("start to batchWrite itemes  ", actionItemMappedDynamoArray);
  
        let response = await ddbDocClient.batchWrite(params);
  
        console.log("response received from dynamo is ", response);
        console.log("response.UnprocessedItems ", response.UnprocessedItems);
  
        // some items are written (but maybe not all of them)
        if (
          response.UnprocessedItems &&
          response.UnprocessedItems.PocFutureAction
        ) {
          console.log(
            "There are unprocessed items ",
            response.UnprocessedItems.PocFutureAction
          );
  
          splicedFutureActionsArray.forEach(function (splicedFutureAction) {
            if (
              response.UnprocessedItems.PocFutureAction.filter(
                ((unprocessedFutureAction) =>
                  unprocessedFutureAction.actionId ==
                  splicedFutureAction.actionId).length !== 0
              )
            ) {
              futureActions.push(splicedFutureAction); //Se fallisce nella put, l'action viene reinserita tra quelle da inviare
            }
          });
  
          return futureActions;
        }
      } catch (exceptions) {
        console.error("Dynamo cannot write items. Exception is", exceptions);
        futureActions = futureActions.concat(splicedFutureActionsArray);
        console.log(
          "splicedFutureActionsArray length ",
          splicedFutureActionsArray.length
        );
        console.log(
          "splicedFutureActionsArray ",
          JSON.stringify(splicedFutureActionsArray)
        );
        console.log("futureActions length", futureActions.length);
        console.log("futureActions ", JSON.stringify(futureActions));
        return futureActions;
      }
    }
  
    console.log(
      "writeMessagesToDynamo completed. futureActions length is",
      futureActions
    );
    return futureActions;
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
