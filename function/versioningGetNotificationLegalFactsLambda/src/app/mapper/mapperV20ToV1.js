exports.transformFromV20ToV1 = function (responseV20) {
  console.log("transformFromV20ToV1");
  let responseV1 = responseV20.filter(item => item.legalFactsId.category !== 'NOTIFICATION_CANCELLED');
  return responseV1;
}