// converte la risposta V2.x a V1
const { transformFromV20ToV1 } = require('./mapper/mapperV20ToV1.js');

const axios = require("axios");
const axiosRetry = require("axios-retry").default;

exports.versioning = async (event, context) => {
  console.log("event: ", JSON.stringify(event));

  const IUN = event.pathParameters["iun"];
  const path = `/${IUN}/legal-facts`;

  const error = validateEndpoint(event, path);
  if (error)
    return error;

  console.log("pn-versioningGetNotificationLegalFactsLambda function started");

  const url = `${process.env.PN_DELIVERYPUSH_URL}${path}`;

  const attemptTimeout = `${process.env.ATTEMPT_TIMEOUT_SEC}` * 1000;

  const numRetry = `${process.env.NUM_RETRY}`;

  console.log(`attemptTimeout ${attemptTimeout} millis  ${numRetry} retry`);

  axiosRetry(axios, {
    retries: numRetry,
    shouldResetTimeout: true,
    retryCondition: (error) => {
      return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.code === 'ECONNABORTED';
    },
    onRetry: retryCallback,
    onMaxRetryTimesExceeded: retryTimesExceededCallback
  });

  // ora è necessario sapere da che versione sto invocando, per prendere le decisioni corrette.
  let version = getVersion(event);

  const headers = getHeaders(event);

  console.log("calling ", url);
  let response;
  try {
    response = await axios.get(url, { headers: headers, timeout: attemptTimeout });

    let transformedObject = response.data;
    switch (version) {
      case 10:
        transformedObject = transformFromV20ToV1(response.data);
        break;
      default:
        console.error('Invalid version ', version)
        break;
    }

    const ret = {
      statusCode: response.status,
      body: JSON.stringify(transformedObject),
    };
    return ret;
  } catch (error) {
    if (error.response) {
      console.log("risposta negativa: ", error.response.data);
      const ret = {
        statusCode: error.response.status,
        body: JSON.stringify(error.response.data)
      };
      return ret;
    } else {
      console.warn("Error on url " + url, error)
      return {
        statusCode: 500,
        body: JSON.stringify(generateProblem(500, error.message))
      }
    }
  }

  function retryCallback(retryCount, error, requestConfig) {
    console.warn(`Retry num ${retryCount} - error:${error.message}`);
  }

  function retryTimesExceededCallback(error, retryCount) {
    console.warn(`Retries exceeded: ${retryCount} - error:${error.message}`);
  }
};

function validateEndpoint(event, path) {
  if (
    !event["path"].startsWith("/delivery-push/") ||
    event["httpMethod"].toUpperCase() !== "GET"
  ) {
    console.log(
      "ERROR ENDPOINT ERRATO: {path, httpMethod} ",
      event["path"],
      event["httpMethod"]
    );
    const err = {
      statusCode: 502,
      body: JSON.stringify(generateProblem(502, "ENDPOINT ERRATO"))
    };

    return err;
  }
}

function getHeaders(event) {
  const headers = JSON.parse(JSON.stringify(event["headers"]));
  headers["x-pagopa-pn-src-ch"] = "B2B";

  if (event.requestContext.authorizer["cx_groups"]) {
    headers["x-pagopa-pn-cx-groups"] =
      event.requestContext.authorizer["cx_groups"];
  }
  if (event.requestContext.authorizer["cx_id"]) {
    headers["x-pagopa-pn-cx-id"] = event.requestContext.authorizer["cx_id"];
  }
  if (event.requestContext.authorizer["cx_role"]) {
    headers["x-pagopa-pn-cx-role"] = event.requestContext.authorizer["cx_role"];
  }
  if (event.requestContext.authorizer["cx_type"]) {
    headers["x-pagopa-pn-cx-type"] = event.requestContext.authorizer["cx_type"];
  }
  if (event.requestContext.authorizer["cx_jti"]) {
    headers["x-pagopa-pn-jti"] = event.requestContext.authorizer["cx_jti"];
  }
  if (event.requestContext.authorizer["sourceChannelDetails"]) {
    headers["x-pagopa-pn-src-ch-details"] =
      event.requestContext.authorizer["sourceChannelDetails"];
  }
  if (event.requestContext.authorizer["uid"]) {
    headers["x-pagopa-pn-uid"] = event.requestContext.authorizer["uid"];
  }
  return headers;
}

function getVersion(event) {
  let version = 10;

  if (event["path"].startsWith("/delivery-push/v2.0/")) {
    version = 20;
  }

  console.log("version: ", version);
  return version;
}

function generateProblem(status, message) {
  return {
    status: status,
    errors: [
      {
        code: message
      }
    ]
  }
}
