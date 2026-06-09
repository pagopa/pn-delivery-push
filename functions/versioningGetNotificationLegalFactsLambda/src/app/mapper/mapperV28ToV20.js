exports.transformFromV28ToV20 = function (responseV28) {
  console.log("transformFromV28ToV20");
  let responseV20 = responseV28.filter(item => item.legalFactsId.category !== 'ANALOG_DELIVERY_TIMEOUT');
  return responseV20;
}