const nDaysFromNowAsUNIXTimestamp = (n) => {
  // if string make it number
  n = +n;

  if (n <= 0) {
    return 0;
  }
  const date = new Date();
  date.setDate(date.getDate() + n);
  return Math.floor(date.getTime() / 1000);
};

const isPersistenceEnabled = () => {
  const actionLambdaStartEpoch = process.env.ACTION_LAMBDA_ENABLED_START;
  const actionLambdaEndEpoch = process.env.ACTION_LAMBDA_ENABLED_END;

  if (!actionLambdaStartEpoch || !actionLambdaEndEpoch) {
    console.warn(
      "ACTION_LAMBDA_START_EPOCH or ACTION_LAMBDA_END_EPOCH not set, persistence is disabled"
    );
    return false;
  }

  const startEpoch = parseInt(actionLambdaStartEpoch, 10);
  const endEpoch = parseInt(actionLambdaEndEpoch, 10);
  const currentEpoch = Date.now(); // Get current time in milliseconds

  if (currentEpoch < startEpoch || currentEpoch > endEpoch) {
    console.info(
      `Current epoch ${currentEpoch} is outside of the range [${startEpoch}, ${endEpoch}], persistence is disabled`
    );
    return false;
  }

  console.info(
    `Current epoch ${currentEpoch} is within the range [${startEpoch}, ${endEpoch}], persistence is enabled`
  );
  return true;
}

exports.nDaysFromNowAsUNIXTimestamp = nDaysFromNowAsUNIXTimestamp;
exports.isPersistenceEnabled = isPersistenceEnabled;