exports.createStreamRequestV22 = (requestBody) => {
    console.log("transformStreamRequestFromV10ToV23");
    return {
        title: requestBody.title,
        eventType: requestBody.eventType,
        groups: [],
        filterValues: requestBody.filterValues
    };
}