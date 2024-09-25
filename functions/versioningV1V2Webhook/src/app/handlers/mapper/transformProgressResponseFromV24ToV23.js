
exports.createProgressResponseV23 = (responseBody) => {
    console.debug("createProgressResponseV23")

    const element = responseBody.element;
    
    if( element.ingestionTimestamp || element.notificationSentAt ) {
        console.log("transformTimeline - rm ingestionTimestamp e notificationSentAt")
        delete element.notificationSentAt;
        delete element.ingestionTimestamp;
    }

    return responseBody;
}