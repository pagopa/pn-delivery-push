const utils = require("./utils");

async function getQueueName(actionType, details, parameterStoreName) {
    const completeActionType = getCompleteActionType(actionType, details);
    console.log("Complete action type: ", completeActionType);
    const queueName = await getQueueFromParameterStore(completeActionType, parameterStoreName);
    return queueName;
}

function getCompleteActionType(actionType, details) {
    if(!details){
        return actionType;
    }
    if (actionType === 'DOCUMENT_CREATION_RESPONSE' && details.documentCreationType === 'SENDER_ACK') {
        return actionType + '_SENDER_ACK';
    }
    return actionType;
}

async function getQueueFromParameterStore(completeActionType, parameterStoreName) {    
    const parameterStoreMap = await utils.getParameterFromLayer(parameterStoreName);
    const jsonMap = JSON.parse(parameterStoreMap);
    const foundObject = jsonMap.find(item => item.tipologiaAzione === completeActionType);
    if (!foundObject) {
        console.error("Unable to find queue for action type: ", completeActionType);
        throw new Error("Unable to find queue");
    }
    console.log("found object: ", foundObject);
    return foundObject.queueName;
}



module.exports = {
    getQueueName
}