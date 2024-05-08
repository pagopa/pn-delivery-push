const { isRecordToSend, isFutureAction } = require("./utils/utils.js");
const { writeMessagesToQueue } = require("./sqs/writeToSqs.js");
const { writeMessagesToDynamo } = require("./dynamo/writeToDynamo.js");

const { unmarshall } = require("@aws-sdk/util-dynamodb");

const handleEvent = async (event, context) => {
  console.log(JSON.stringify(event, null, 2));

  let futureActions = [];
  let immediateActions = [];

  for (var i = 0; i < event.Records.length; i++) {
    let record = event.Records[i];
    let sequenceNumber = record.kinesis.sequenceNumber;
    let decodedRecord = decodeBase64(record.kinesis.data);

    if (isRecordToSend(decodedRecord)) {
      const action = mapMessageFromKinesisToAction(
        decodedRecord,
        sequenceNumber
      );
      
      if (isFutureAction(action.notBefore)) {
        console.log("Is future action ", action.actionId);
        futureActions.push(action);
      } else {
        console.log("Is immediate action ", action.actionId);
        immediateActions.push(action);
      }
    }
  }

  let notSendedImmediateActions = [];
  if (immediateActions.length > 0){
    notSendedImmediateActions = await writeMessagesToQueue(
      immediateActions,
      context
    );
  }
  else {
    console.log("No ImmediateAction to send");
  }
  
  let notSendedFutureActions = [];
  if (futureActions.length > 0){
    notSendedFutureActions = await writeMessagesToDynamo(
      futureActions,
      context
    );
  } else {
    console.log("No futureActions to write");
  }

  let notSendedActions = notSendedImmediateActions.concat(
    notSendedFutureActions
  );
  console.log("notSendedActions ", notSendedActions);

  const result = {
    batchItemFailures: [],
  };

  if (notSendedActions.length !== 0) {
    notSendedActions.forEach((element) =>
      result.batchItemFailures.push({ itemIdentifier: element.kinesisSeqNo })
    );
  }

  console.log("result returned to kinesis is ", result);
  return result;
};

function mapMessageFromKinesisToAction(record, sequenceNumber) {
  console.log("il record e", record);
  let action = record.dynamodb.NewImage;
  console.log("action", action);
  const regularAction = unmarshall(action);
  regularAction.kinesisSeqNo = sequenceNumber;
  console.log("regularAction", regularAction);
  return regularAction;
}

function decodeBase64(encodedRecord) {
  var decodedString = Buffer.from(encodedRecord, "base64").toString();
  let decodedJson = JSON.parse(decodedString);
  console.log("decodedJson", decodedJson);
  return decodedJson;
}

module.exports = { handleEvent };
