exports.createStreamCreationRequestV22 = (requestBody) => {
    console.log("transformStreamCreationRequestFromV10ToV23");
    return {
        title: requestBody.title,
        eventType: requestBody.eventType,
        groups: null,
        filterValues: requestBody.filterValues,
        replacedStreamId : null
    }
}