/**
 * convert kinesis object to json object
 * @returns {obj} json obkject
 */
exports.parseKinesisObjToJsonObj = (elToParse) => {
  const keysToBypass = [
    "N",
    "M",
    "S",
    "BOOL",
    "SS",
    "NS",
    "BS",
    "L",
    "NULL",
    "B",
  ];
  if (elToParse === null || elToParse === undefined) {
    return elToParse;
  } else if (Array.isArray(elToParse)) {
    const elParsed = [];
    for (const el of elToParse) {
      elParsed.push(this.parseKinesisObjToJsonObj(el));
    }
    return elParsed;
  } else if (typeof elToParse === "object") {
    let elParsed = {};
    for (const [key, value] of Object.entries(elToParse)) {
      if (keysToBypass.includes(key)) {
        elParsed = this.parseKinesisObjToJsonObj(value);
        continue;
      }
      elParsed[key] = this.parseKinesisObjToJsonObj(value);
    }
    return elParsed;
  }
  // neither object or array (string, number or boolean)
  return elToParse;
};
