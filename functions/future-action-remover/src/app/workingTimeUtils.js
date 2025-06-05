const config = require("config");

const getWorkingTime = () => {
  const workingTime = config.get("featureFlag");
  console.info(
    "[FUTURE_ACTIONS_REMOVER]",
    `Operating window is from [${workingTime.start}, ${workingTime.end}]`
  );
  return workingTime;
};

module.exports = { getWorkingTime };
