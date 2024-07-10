const TOLLERANCE_IN_MILLIS = 3000;

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

    return datePlusDays.toISOString();
}

module.exports = { isRecordToSend, isTimeToLeave, isFutureAction, addDaysToDate};
