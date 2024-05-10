const utils = require("./utils");

async function getQueueNameFromParameterStore(actionType, details, parameterStoreName) {
    const completeActionType = getCompleteActionType(actionType, details);
    console.log("Complete action type: ", completeActionType);
    const queueName = await getQueueFromParameterStore(completeActionType, parameterStoreName);
    return queueName;
}

async function getQueueName(actionType, details, envVarName) {
    const completeActionType = getCompleteActionType(actionType, details);
    console.log("Complete action type: ", completeActionType);
    const queueName = getQueueFromEnvVar(completeActionType, envVarName);
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

function getQueueFromEnvVar(completeActionType, envVarName) {
    const envVarValue = process.env[envVarName];
    var jsonMap;
    try {
        jsonMap = JSON.parse(envVarValue);
    } catch(ex) {
        console.error("Invalid env var value: ", envVarValue);
        throw new Error("Invalid env var value");
    }
    
    const foundObject = jsonMap.find(item => item.tipologiaAzione === completeActionType);
    if (!foundObject) {
        console.error("Unable to find queue for action type: ", completeActionType);
        throw new Error("Unable to find queue");
    }
    console.log("found object: ", foundObject);
    return foundObject.queueName;
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
    getQueueNameFromParameterStore,
    getQueueName
}