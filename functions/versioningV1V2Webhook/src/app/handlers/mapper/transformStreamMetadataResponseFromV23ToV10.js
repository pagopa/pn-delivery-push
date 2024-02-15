exports.createStreamMetadataResponseV10 = (responseBody) => {
    console.log("transformStreamMetadataResponseFromV23ToV10");
    return {
        title: responseBody.title,
        eventType: responseBody.eventType,
        filterValues: responseBody.filterValues,
        streamId : responseBody.streamId,
        activationDate : responseBody.activationDate
    };
}