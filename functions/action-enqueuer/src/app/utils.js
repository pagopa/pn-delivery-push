const axios = require("axios");

async function retryWithDelay(fn, delay, retries) {
  try {
    return await fn();
  } catch (err) {
    if (retries > 0) {
      await new Promise((r) => setTimeout(r, delay));
      return await retryWithDelay(fn, delay, retries - 1);
    } else {
      throw err;
    }
  }
}

async function getParameterFromLayer(parameterName) {
  return await retryWithDelay(
    () => innerGetParameterFromLayer(parameterName),
    1000,
    3
  );
}

async function innerGetParameterFromLayer(parameterName) {
  try {
    const response = await axios.get(
      `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(
        parameterName
      )}`,
      {
        headers: {
          "X-Aws-Parameters-Secrets-Token": process.env.AWS_SESSION_TOKEN,
        },
      }
    );
    return response.data.Parameter.Value;
  } catch (err) {
    console.error("Unable to get parameter ", err);
    throw new Error("Unable to get parameter");
  }
}

module.exports = {
  getParameterFromLayer,
};
