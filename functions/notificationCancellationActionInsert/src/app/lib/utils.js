exports.nDaysFromNowAsUNIXTimestamp = (n) => {
  if (n <= 0) {
    return 0;
  }
  const date = new Date();
  date.setDate(date.getDate() + n);
  return Math.floor(date.getTime() / 1000);
};
