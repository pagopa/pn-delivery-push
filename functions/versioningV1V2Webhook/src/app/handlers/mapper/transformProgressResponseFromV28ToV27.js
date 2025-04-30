exports.createProgressResponseV27 = (responseBody) => {
    console.debug("createProgressResponseV24")

    const element = responseBody.element;

    if(element.category === 'REQUEST_ACCEPTED') {
        removeNotificationRequestParamsFromDetail(element);
    }
    
    if (element.category === 'REQUEST_REFUSED') {
        removeNotificationRequestParamsFromDetail(element);
        if(element.details?.refusalReasons && element.details.refusalReasons.length > 0) {
            // Remove recIndex from refusalReasons if it exists
            element.details.refusalReasons = element.details.refusalReasons.map(({recIndex, ...rest}) => {
                return rest;  
            });
        }
    }

    return responseBody;
}

function removeNotificationRequestParamsFromDetail(element) {
    delete element?.details?.notificationRequestId;
    delete element?.details?.paProtocolNumber;
    delete element?.details?.idempotenceToken;
}