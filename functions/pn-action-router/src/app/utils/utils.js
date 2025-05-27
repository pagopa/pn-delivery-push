const config = require("config");
const TOLLERANCE_IN_MILLIS = config.get("RUN_TOLLERANCE_IN_MILLIS");
const TTL_TIME_TO_ADD_IN_MILLIS = config.get("TTL_TIME_TO_ADD_IN_MILLIS");

function isTimeToLeave (context){
    return context.getRemainingTimeInMillis() < TOLLERANCE_IN_MILLIS;
}

function isRecordToSend(record) {
    console.log("eventName ", record.eventName);
    if (record.eventName != "INSERT" ||
            (record.dynamodb.NewImage.notToHandle && (record.dynamodb.NewImage.notToHandle.BOOL == true || record.dynamodb.NewImage.notToHandle.S == "true"))
        ){
        return false;
    }
    // il record è buono e va processato e inviato
    return true;
}

function isFutureAction(notBefore) {
    var date = new Date();
    let isoDateNow = date.toISOString();
    console.log("notBefore", notBefore, "and isoDateNow ", isoDateNow);
    return isoDateNow < notBefore;
}

function addDaysToDate(startDate, daysToAdd) {
    console.log('startDate is ', startDate)

    var date = new Date(startDate)
    let millisecond = date.getTime();

    let datePlusDays =  new Date(millisecond + TTL_TIME_TO_ADD_IN_MILLIS * daysToAdd)
    console.log('datePlusDays is ', datePlusDays)
    const unixTimestamp = Math.floor(datePlusDays.getTime() / 1000);
    console.log('unixTimestamp is ', unixTimestamp)

    return unixTimestamp;
}

const isLambdaDisabled = (featureFlag) => {
  const currentDate = new Date().toISOString();
  const { start, end } = featureFlag;

  return currentDate < start || currentDate > end;
};

module.exports = { isRecordToSend, isTimeToLeave, isFutureAction, addDaysToDate, isLambdaDisabled};
