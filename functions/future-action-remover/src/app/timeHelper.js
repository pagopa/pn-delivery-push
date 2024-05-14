const { DateTime } = require("luxon");
const { InvalidDateException } = require("./exceptions");

const getCleanTime = (d) => d.set({ second: 0, millisecond: 0 });

const nextTimeSlot = (d) => {
  if (!d.isValid) throw new InvalidDateException(d);
  return d.plus({ minute: 1 });
};

const isAfter = (d, d1) => {
  if (!d.isValid) throw new InvalidDateException(d);
  if (!d1.isValid) throw new InvalidDateException(d1);

  return d > d1;
};

const isAfterEq = (d, d1) => {
  if (!d.isValid) throw new InvalidDateException(d);
  if (!d1.isValid) throw new InvalidDateException(d1);

  return d >= d1;
};

const parseISO = (str) => {
  const converted = DateTime.fromISO(str, { zone: "UTC" });
  if (!converted.isValid) throw new InvalidDateException(str);
  return getCleanTime(converted);
};

const actTime = () => getCleanTime(DateTime.local().toUTC());

const toString = (d) =>
  d.toISO({
    suppressMilliseconds: true,
    suppressSeconds: true,
    includeOffset: false,
  });

module.exports = {
  nextTimeSlot,
  isAfter,
  isAfterEq,
  parseISO,
  actTime,
  toString,
};
console.log(toString(actTime()));
