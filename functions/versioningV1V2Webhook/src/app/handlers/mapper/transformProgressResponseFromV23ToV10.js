const SAFE_STORAGE_URL_PREFIX = "safestorage://";

exports.createProgressResponseV10 = (responseBody) => {
    console.log("transformProgressResponseFromV23ToV10");

    const element = responseBody.element;

    const timestampV10 = element.timestamp;

    const timelineEventCategoryV10 = element.category;

    let legalFactsIdsV10 = null;
    if(element.legalFactsIds)
        legalFactsIdsV10 = transformLegalfactIdsFromV23ToV10(element.legalFactsIds);

    //estraggo altri dati da details
    const timelineElementDetails = element.details;

    let recipientIndexV10 = null;
    if (timelineElementDetails.recIndex)
        recipientIndexV10 = timelineElementDetails.recIndex;

    let analogCostV10 = null;
    if( element.category === "SEND_ANALOG_DOMICILE" || element.category === "SEND_SIMPLE_REGISTERED_LETTER")
        analogCostV10 = timelineElementDetails.analogCost;

    const channelV10 = transformChannelFromV23ToV10(timelineElementDetails, element.category);

    let validationErrorsV10 = null;
    if( element.category === "REQUEST_REFUSED")
        validationErrorsV10 = transformValidationErrorsFromV23ToV10(timelineElementDetails.refusalReasons);

    return {
        eventId: responseBody.eventId,
        notificationRequestId: responseBody.notificationRequestId,
        iun: responseBody.iun,
        newStatus: responseBody.newStatus,
        timestamp: timestampV10,
        timelineEventCategory: timelineEventCategoryV10,
        recipientIndex: recipientIndexV10,
        analogCost: analogCostV10,
        channel: channelV10,
        legalFactsIds: legalFactsIdsV10,
        validationErrors: validationErrorsV10
    }
}

function transformChannelFromV23ToV10(detailsV23, category){

    if(category === "SEND_ANALOG_FEEDBACK" || category ===  "SEND_ANALOG_DOMICILE" || category === "PREPARE_ANALOG_DOMICILE" || category === "SEND_ANALOG_PROGRESS")
        return detailsV23.serviceLevel;

    if(category === "PREPARE_SIMPLE_REGISTERED_LETTER" || category ===  "SEND_SIMPLE_REGISTERED_LETTER")
        return "SIMPLE_REGISTERED_LETTER";

    if (category === "SEND_DIGITAL_DOMICILE" || category === "SEND_DIGITAL_PROGRESS" || category === "SEND_DIGITAL_FEEDBACK" )
        return "PEC";

    if (category === "SEND_COURTESY_MESSAGE")
        return detailsV23.digitalAddress.type;
}

function transformLegalfactIdsFromV23ToV10(legalFactsIdsV23){

    let legalfactIdsV10 = [];

    legalFactsIdsV23.forEach(legalfactId => {
        legalfactIdsV10.push(
            legalfactId.key.replace( SAFE_STORAGE_URL_PREFIX , "")
        );
    });
    return legalfactIdsV10;
}

function transformValidationErrorsFromV23ToV10(refusalReasonsV23){

    let validationErrorsV10 = [];

    refusalReasonsV23.forEach(notificationRefusedError => {
        validationErrorsV10.push(
            notificationRefusedError
        )
    });
    return validationErrorsV10;
}