
exports.createProgressResponseV24 = (responseBody) => {
    console.debug("createProgressResponseV24")

    const element = responseBody.element;
    
    if (element.category === 'NOTIFICATION_CANCELLED') {
        element.legalFactsIds = [];
    }

    return responseBody;
}