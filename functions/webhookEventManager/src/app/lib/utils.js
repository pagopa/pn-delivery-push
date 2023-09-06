/**
 * produces, starting from a string containing the IUN, a numeric sequence from 00 to 10 (11 total values)
 * @param {string} iun_string the string containing the IUN
 * @returns {string} "00" to "10"
 */
exports.twoNumbersFromIUN = (iun_string) => {
  const controlLetter = this.controlLetterFromIUN(iun_string).toUpperCase();

  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  const letterIndex = alphabet.indexOf(controlLetter.toUpperCase());
  return (letterIndex % 11).toString().padStart(2, "0"); // "00" to "10", not to "09", so 11, not 10
};

/**
 * extracts the control letter from the IUN string
 * @param {string} iun_string the string containing the IUN
 * @returns {string} 'A' to 'Z'
 */
exports.controlLetterFromIUN = (iun_string) => {
  const parts = iun_string.split("-");
  return parts[parts.length - 2];
};

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

exports.initTtlSlaTimes = () => {
  const ttlSlaTimes = {
    // The default values are stored in the microservice.yml Parameters section.
    // The values here are only for the initialization of the object and will be overwritten.
    ALARM_TTL_VALIDATION: 0.5,
    ALARM_TTL_REFINEMENT: 110,
    ALARM_TTL_SEND_PEC: 2,
    ALARM_TTL_SEND_PAPER_AR_890: 100,
    ALARM_TTL_SEND_AMR: 2,
    SLA_EXPIRATION_VALIDATION: 1,
    SLA_EXPIRATION_REFINEMENT: 120,
    SLA_EXPIRATION_SEND_PEC: 2,
    SLA_EXPIRATION_SEND_PAPER_AR_890: 100,
    SLA_EXPIRATION_SEND_AMR: 2,
    INVOICING_TTL_DAYS: 365,
  };

  ttlSlaTimes.ALARM_TTL_VALIDATION =
    process.env.ALARM_TTL_VALIDATION || ttlSlaTimes.ALARM_TTL_VALIDATION;
  ttlSlaTimes.ALARM_TTL_REFINEMENT =
    process.env.ALARM_TTL_REFINEMENT || ttlSlaTimes.ALARM_TTL_REFINEMENT;
  ttlSlaTimes.ALARM_TTL_SEND_PEC =
    process.env.ALARM_TTL_SEND_PEC || ttlSlaTimes.ALARM_TTL_SEND_PEC;
  ttlSlaTimes.ALARM_TTL_SEND_PAPER_AR_890 =
    process.env.ALARM_TTL_SEND_PAPER_AR_890 ||
    ttlSlaTimes.ALARM_TTL_SEND_PAPER_AR_890;
  ttlSlaTimes.ALARM_TTL_SEND_AMR =
    process.env.ALARM_TTL_SEND_AM || ttlSlaTimes.ALARM_TTL_SEND_AM;
  ttlSlaTimes.SLA_EXPIRATION_VALIDATION =
    process.env.SLA_EXPIRATION_VALIDATION ||
    ttlSlaTimes.SLA_EXPIRATION_VALIDATION;
  ttlSlaTimes.SLA_EXPIRATION_REFINEMENT =
    process.env.SLA_EXPIRATION_REFINEMENT ||
    ttlSlaTimes.SLA_EXPIRATION_REFINEMENT;
  ttlSlaTimes.SLA_EXPIRATION_SEND_PEC =
    process.env.SLA_EXPIRATION_SEND_PEC || ttlSlaTimes.SLA_EXPIRATION_SEND_PEC;
  ttlSlaTimes.SLA_EXPIRATION_SEND_PAPER_AR_890 =
    process.env.SLA_EXPIRATION_SEND_PAPER_AR_890 ||
    ttlSlaTimes.SLA_EXPIRATION_SEND_PAPER_AR_890;
  ttlSlaTimes.SLA_EXPIRATION_SEND_AMR =
    process.env.SLA_EXPIRATION_SEND_AMR || ttlSlaTimes.SLA_EXPIRATION_SEND_AMR;
  ttlSlaTimes.INVOICING_TTL_DAYS =
    process.env.INVOICING_TTL_DAYS || ttlSlaTimes.INVOICING_TTL_DAYS;

  return ttlSlaTimes;
};
