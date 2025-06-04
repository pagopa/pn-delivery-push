const config = require("config");

const getWorkingTime = async () => {
  const workingTime = config.get("featureFlag");
  console.info(
    "[ACTION_ROUTER]",
    `Operating window is from [${workingTime.start}, ${workingTime.end}]`
  );
  return workingTime;
};

const insideWorkingWindow = (action, startWw, endWs) => {
  if (action && action?.createdAt)
    return action.createdAt < endWs && action.createdAt >= startWw;
  console.info("[ACTION_ROUTER]", `Action doesn't have createdAt attribute`, action);
  return true;
};

module.exports = { insideWorkingWindow, getWorkingTime };
