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
    var date = new Date(startDate);
    date.setDate(date.getDate() + daysToAdd);
    return date;
}

var date = new Date();

console.log(date.addDays(5));


module.exports = { isRecordToSend, isTimeToLeave, isFutureAction, addDaysToDate};
