const utils = require("./utils");

async function getQueueNameFromParameterStore(actionType, details, parameterStoreName) {
    const completeActionType = getCompleteActionType(actionType, details);
    console.log("Complete action type: ", completeActionType);
    const queueName = await getQueueFromParameterStore(completeActionType, parameterStoreName);
    return queueName;
}

async function getQueueName(actionType, details, envVarMapQueueName) {
    const completeActionType = getCompleteActionType(actionType, details);
    console.log("Complete action type: ", completeActionType);
    const queueName = getQueueNameFromEnvVar(completeActionType, envVarMapQueueName);
    return queueName;
}

async function getQueueUrl(actionType, details, envVarMapQueueName, envVarMapUrlName) {
    const queueName = await getQueueName(actionType, details, envVarMapQueueName);
    const queueUrl = await getQueueUrlFromEnvVar(queueName, envVarMapUrlName);
    return queueUrl;
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

async function getQueueUrlFromEnvVar(queueName, envVarMapUrlName) {
    const envVarValue = process.env[envVarMapUrlName];
    var jsonMap;
    try {
        jsonMap = JSON.parse(envVarValue);
    } catch(ex) {
        console.error("Invalid env var value: ", envVarValue);
        throw new Error("Invalid env var value");
    }
    const queueUrl = jsonMap[queueName];
    if (!queueUrl) {
        console.error("Unable to find queue url for queue: ", queueName);
        throw new Error("Unable to find queue url");
    }
    console.log("found queueUrl: ", queueUrl);
    return queueUrl;
}

function getQueueNameFromEnvVar(completeActionType, envVarMapQueueName) {
    const envVarValue = process.env[envVarMapQueueName];
    var jsonMap;
    try {
        jsonMap = JSON.parse(envVarValue);
    } catch(ex) {
        console.error("Invalid env var value: ", envVarValue);
        throw new Error("Invalid env var value");
    }
    const queueName = jsonMap[completeActionType];
    if (!queueName) {
        console.error("Unable to find queue for action type: ", completeActionType);
        throw new Error("Unable to find queue");
    }
    console.log("found queueName: ", queueName);
    return queueName;
}

async function getQueueFromParameterStore(completeActionType, parameterStoreName) {    
    const parameterStoreMap = await utils.getParameterFromLayer(parameterStoreName);
    const jsonMap = JSON.parse(parameterStoreMap);
    const queueName = jsonMap[completeActionType]
    if (!queueName) {
        console.error("Unable to find queue for action type: ", completeActionType);
        throw new Error("Unable to find queue");
    }
    console.log("found queueName: ", queueName);
    return queueName;
}



module.exports = {
    getQueueNameFromParameterStore,
    getQueueName,
    getQueueUrl
}