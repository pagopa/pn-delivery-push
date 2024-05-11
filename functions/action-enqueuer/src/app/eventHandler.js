const { putMessages } = require("./sqsFunctions");
const { unmarshall } = require("@aws-sdk/util-dynamodb");
const config = require("config");

const { insideWorkingWindow, getWorkingTime } = require("./workingTimeUtils");
const { getQueueName } = require("./utils");

const TOLLERANCE_IN_MILLIS = config.get("RUN_TOLLERANCE_IN_MILLIS");

const isTimeToLeave = (context) =>
  context.getRemainingTimeInMillis() < TOLLERANCE_IN_MILLIS;

function decodeBase64(data) {
  const payload = Buffer.from(data, "base64").toString("ascii");
  let decodedPayload = JSON.parse(payload);
  return decodedPayload;
}

const isRecordToSend = (record) => record.eventName === "REMOVE";

function mapMessageFromKinesisToAction(record) {
  let action = record.dynamodb.OldImage;
  //console.debug("[ACTION_ENQUEUER]", "Action", action);
  const regularAction = unmarshall(action);
  //console.debug("[ACTION_ENQUEUER]", "regularAction", regularAction);
  return regularAction;
}

// const getRecordCreation = (record) =>
//   new Date(record.dynamodb.ApproximateCreationDateTime).toISOString();

const sendMessages = async (destinationEndpoint, actions, timeoutFn) => {
  const notSendedResult = {
    batchItemFailures: [],
  };

  if (!destinationEndpoint) {
    actions.forEach((element) =>
      notSendedResult.batchItemFailures.push({ itemIdentifier: element.seqNo })
    );
    console.error(
      "[ACTION_ENQUEUER]",
      `Destination Enpoint ${destinationEndpoint} not valid`,
      notSendedResult
    );
    return notSendedResult;
  }

  let sqsParams = { endpoint: destinationEndpoint };
  console.debug(
    "[ACTION_ENQUEUER]",
    `Sending ${actions.length} actions to SQS queue ${sqsParams.endpoint}`
  );

  const notSended = await putMessages(sqsParams, actions, timeoutFn);
  if (notSended.length !== 0) {
    notSended.forEach((element) =>
      notSendedResult.batchItemFailures.push({ itemIdentifier: element.seqNo })
    );
    console.error("[ACTION_ENQUEUER]", "NOT SENDED", notSendedResult);
  }
  return notSendedResult;
};

async function handleEvent(event, context) {
  const emptyResult = {
    batchItemFailures: [],
  };

  const workingTime = await getWorkingTime();

  console.log("[ACTION_ENQUEUER]", "Started");
  console.log("[ACTION_ENQUEUER]", "Event DATA", event);
  if (!event.Records) {
    console.warn("[ACTION_ENQUEUER]", "No Records to process");
    return emptyResult;
  }
  const isTimedOut = () => isTimeToLeave(context);

  let actions = [];
  let lastDestination;
  for (let i = 0; i < event.Records.length; i++) {
    let record = event.Records[i];
    let decodedRecord = decodeBase64(record.kinesis.data);
    console.debug("DECODED!", JSON.stringify(decodedRecord));
    if (isRecordToSend(decodedRecord)) {
      console.debug("TO ELEBORATE");
      const action = mapMessageFromKinesisToAction(decodedRecord);

      // feature flag check
      if (!insideWorkingWindow(action, workingTime.start, workingTime.end))
        continue;

      console.debug("EVENT IS INSIDE WORKING WINDOW");
      action.seqNo = record.kinesis.sequenceNumber;

      let currentDestination = getQueueName(
        action?.type,
        action?.details,
        "TEST"
      );
      //  destination changed
      if (lastDestination && currentDestination != lastDestination) {
        // send records to previous destination
        const notSended = await sendMessages(
          lastDestination,
          actions,
          isTimedOut
        );
        if (notSended.batchItemFailures.length != 0) {
          return notSended;
        }
        actions = [];
      }
      //destination endpoint update
      lastDestination = currentDestination;
      actions.push(action);
    } else {
      console.log("[ACTION_ENQUEUER]", "Discarded record", decodedRecord);
    }
  }
  if (actions.length !== 0) {
    console.debug(
      "[ACTION_ENQUEUER]",
      "Sending last actions",
      JSON.stringify(actions)
    );
    const notSended = await sendMessages(lastDestination, actions, isTimedOut);
    if (notSended.batchItemFailures.length != 0) {
      return notSended;
    }
  }

  console.log("[ACTION_ENQUEUER]", "No more Records to process");
  return emptyResult;

  // ////////
  // x ogni record kinesis
  //   - verifico se è un record da inviare
  //   caso "non è un record da inviare" -> continua (prossimo record)
  //   caso "è un record da inviare"
  //     - verifico  la destinazione
  //     caso "la destinazione è cambiata o è il primo record"
  //       faccio invio dei record in lista usando la destinazione precedente
  //       caso "invio non ha avuto successo": restituisco la lista dei record non inviati ed esco
  //       caso "invio  ha avuto successo": azzero la lista
  //     in ogni caso:
  //       metto in lista di invio l'item attuale
  // caso "la lista dei record da inviare è vuota": esco
  // caso "la lista dei record da inviare non è vuota":
  //   - faccio invio dei record in lista usando l'ultima destinazione trovata
  //   - caso "invio non ha avuto successo": restituisco la lista dei record non inviati ed esco
}

module.exports = { handleEvent };
