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

const isAfterEqStr = (str, str1) => {
  if (!str)
    throw new InvalidDateException(str);

  if (!str1) 
    throw new InvalidDateException(str1);
  
  const firstToDateTime = parseISO(str);
  const secondToDateTime = parseISO(str1);
  return firstToDateTime >= secondToDateTime;
};

const isAfterStr = (str, str1) => {
  if (!str)
    throw new InvalidDateException(str);

  if (!str1) 
    throw new InvalidDateException(str1);
  
  const firstToDateTime = parseISO(str);
  const secondToDateTime = parseISO(str1);
  return firstToDateTime > secondToDateTime;
};

const isEqStr = (str, str1) => {
  if (!str)
    throw new InvalidDateException(str);

  if (!str1) 
    throw new InvalidDateException(str1);
  
  const firstToDateTime = parseISO(str);
  const secondToDateTime = parseISO(str1);
  return firstToDateTime.equals(secondToDateTime);
};

const parseISO = (str) => {
  const converted = DateTime.fromISO(str, { zone: "UTC" });
  if (!converted.isValid) throw new InvalidDateException(str);
  return getCleanTime(converted);
};

const actTime = () => getCleanTime(DateTime.local().toUTC());


// const actTime = () =>{
//   let actualTime = DateTime.local().toUTC();
//   // console.log('actual time is ', actualTime)
//   // const duration = { minutes: 2 };
//   // const newDateTime = actualTime.minus(duration);
//   // console.log('newDateTime time is ', newDateTime)
//  // return getCleanTime(newDateTime);
//  return getCleanTime(actualTime);
// }

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
  isAfterEqStr,
  isEqStr,
  parseISO,
  actTime,
  toString,
  isAfterStr
};
