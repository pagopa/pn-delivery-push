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

module.exports = { isRecordToSend, isTimeToLeave, isFutureAction};
