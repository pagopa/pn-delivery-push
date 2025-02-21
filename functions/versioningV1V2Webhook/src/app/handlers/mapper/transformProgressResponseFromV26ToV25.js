exports.createProgressResponseV25 = (responseBody) => {
    console.debug("createProgressResponseV26")

    delete responseBody.waitForAccepted;

    return responseBody;
}