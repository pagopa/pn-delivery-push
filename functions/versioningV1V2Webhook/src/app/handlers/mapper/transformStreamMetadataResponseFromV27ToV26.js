exports.createStreamMetadataResponseV26 = (responseBody) => {
    console.log("transformStreamMetadataResponseFromV27ToV26");
    responseBody.waitForAccepted = undefined;
    return responseBody;
}