exports.createStreamCreationRequestV26 = (requestBody) => {
    console.log("transformStreamCreationRequestFromV27toV26");
        requestBody.waitForAccepted = undefined;
        return requestBody;
}