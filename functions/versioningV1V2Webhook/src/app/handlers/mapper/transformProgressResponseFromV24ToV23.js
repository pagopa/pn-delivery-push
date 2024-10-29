
exports.createProgressResponseV23 = (responseBody) => {
    console.debug("createProgressResponseV23")

    const element = responseBody.element;
    
    if( element.ingestionTimestamp ||  element.eventTimestamp || element.notificationSentAt ) {
        console.log("transformTimeline - rm ingestionTimestamp, eventTimestamp, notificationSentAt")
        delete element.notificationSentAt;
        delete element.ingestionTimestamp;
        delete element.eventTimestamp;
    }

    return responseBody;
}