const isLambdaDisabled = (featureFlag) => {
  const currentDate = new Date().toISOString();
  const { start, end } = featureFlag;

  return currentDate < start || currentDate > end;
};

module.exports = {
  isLambdaDisabled
};