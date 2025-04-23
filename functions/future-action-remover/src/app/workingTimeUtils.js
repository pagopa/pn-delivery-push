const config = require("config");

const insideWorkingWindow = (action, startWw, endWs) => {
  if (action && action?.notBefore)
    return action.notBefore <= endWs && action.notBefore >= startWw;
  console.error("[FUTURE_ACTIONS_REMOVER]", `Action NOT valid`, action);
  return false;
};

module.exports = { insideWorkingWindow };
