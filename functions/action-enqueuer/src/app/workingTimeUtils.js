const config = require("config");

const getWorkingTime = () => {
  const workingTime = config.get("featureFlag");
  console.info(
    "[ACTION_ENQUEUER]",
    `Operating window is from [${workingTime.start}, ${workingTime.end}]`
  );
  return workingTime;
};

const insideWorkingWindow = (action, startWw, endWs) => {
  if (action && action?.createdAt)
    return action.createdAt < endWs && action.createdAt >= startWw;
  console.info("[ACTION_ENQUEUER]", `Action doesn't have createdAt attribute`, action);
  return true;
};

module.exports = { insideWorkingWindow, getWorkingTime };
