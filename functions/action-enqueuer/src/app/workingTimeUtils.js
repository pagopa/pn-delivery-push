const { getParameterFromLayer } = require("./utils");

const getWorkingTime = async () => {
  const workingTime = await getParameterFromLayer(
    "/pn-delivery-push/featureFlag"
  );
  console.info(
    "[ACTION_ENQUEUER]",
    `Operating window is from [${workingTime.start}, ${workingTime.end}]`
  );
  return workingTime;
};

const insideWorkingWindow = (action, startWw, endWs) => {
  if (action && action?.notBefore)
    return action.notBefore <= endWs && action.notBefore >= startWw;
  console.error("[ACTION_ENQUEUER]", `Action NOT valid`, action);
  return false;
};

module.exports = { insideWorkingWindow, getWorkingTime };
