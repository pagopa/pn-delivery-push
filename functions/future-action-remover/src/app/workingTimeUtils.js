const config = require("config");
const { convertFromEpochToIsoDateTime } = require("./timeHelper.js");

const getWorkingTime = () => {
  const workingTime = config.get("featureFlag");
  console.info(
    "[FUTURE_ACTIONS_REMOVER]",
    `Operating window (in epoch) is from [${workingTime.start}, ${workingTime.end}]`
  );

  let convertedWorkingTime = {
    start: convertFromEpochToIsoDateTime(workingTime.start),
    end: convertFromEpochToIsoDateTime(workingTime.end),
  }

  console.info(
    "[FUTURE_ACTIONS_REMOVER]",
    `Operating window (in ISO) is from [${convertedWorkingTime.start}, ${convertedWorkingTime.end}]`
  );
  return convertedWorkingTime;
};

module.exports = { getWorkingTime };
