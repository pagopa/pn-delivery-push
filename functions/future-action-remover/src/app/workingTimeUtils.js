const config = require("config");

const getWorkingTime = async () => {
  const workingTime = config.get("featureFlag");
  console.info(
    "[FUTURE_ACTIONS_REMOVER]",
    `Operating window is from [${workingTime.start}, ${workingTime.end}]`
  );
  return workingTime;
};

const insideWorkingWindow = (action, startWw, endWs) => {
  if (action && action?.notBefore)
    return action.notBefore <= endWs && action.notBefore >= startWw;
  console.error("[FUTURE_ACTIONS_REMOVER]", `Action NOT valid`, action);
  return false;
};

module.exports = { insideWorkingWindow, getWorkingTime };
