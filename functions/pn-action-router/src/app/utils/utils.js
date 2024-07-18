const config = require("config");
const TOLLERANCE_IN_MILLIS = config.get("RUN_TOLLERANCE_IN_MILLIS");

function isTimeToLeave (context){
    return context.getRemainingTimeInMillis() < TOLLERANCE_IN_MILLIS;
}

function isRecordToSend(record) {
    console.log("eventName ", record.eventName);
    if (record.eventName != "INSERT") return false;
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

    let datePlusDays =  new Date(millisecond + 86400000 * daysToAdd)
    console.log('datePlusDays is ', datePlusDays)
    const unixTimestamp = Math.floor(datePlusDays.getTime() / 1000);
    console.log('unixTimestamp is ', unixTimestamp)

    return unixTimestamp;
}

module.exports = { isRecordToSend, isTimeToLeave, isFutureAction, addDaysToDate};
