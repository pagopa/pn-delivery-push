const getQueueName = (actionType, details, envVarName) => {
  return "https://sqs.eu-central-1.amazonaws.com/830192246553/PocWrite-PocScheduledActionsQueue-vdcddODDraxb";
};

module.exports = {
  getQueueName,
};