process.env.NUM_RETRY = "2";
process.env.ATTEMPT_TIMEOUT_SEC = "1";

const { versioning } = require("../app/eventHandler.js");
const { expect } = require("chai");
const fs = require("fs");
const axios = require('axios');
var MockAdapter = require("axios-mock-adapter");
var mock = new MockAdapter(axios);


describe("eventHandler tests", function () {
  it("statusCode 200 v1.0", async () => {
    const responseJSON = fs.readFileSync("./src/test/response.json");
    let responseObj = JSON.parse(responseJSON);

    process.env = Object.assign(process.env, {
      PN_DELIVERYPUSH_URL: "https://api.dev.notifichedigitali.it/delivery-push",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERYPUSH_URL}/${iunValue}/legal-facts`;

    mock.onGet(url).reply(200, responseObj, { "Content-Type": "application/json" });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: `/delivery-push/${iunValue}/legal-facts`,
      path: "/delivery-push/MOCK_IUN/legal-facts",
      httpMethod: "GET",
    };
    const context = {};

    const response = await versioning(event, context);

    expect(response.statusCode).to.equal(200);
    let resJson = JSON.parse(response.body);
    let isNotificationCancelledPresent = resJson.some(element => element.legalFactsId.category === 'NOTIFICATION_CANCELLED');
    expect(isNotificationCancelledPresent).to.be.false;
  });

  it("statusCode 500 - fetch problem", async () => {

    process.env = Object.assign(process.env, {
      PN_DELIVERYPUSH_URL: "https://api.dev.notifichedigitali.it/delivery-push",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERYPUSH_URL}/${iunValue}/legal-facts`;

    mock.onGet(url).reply(500, { error: "ERROR" }, { "Content-Type": "application/json" });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: `/delivery-push/${iunValue}/legal-facts`,
      path: "/delivery-push/MOCK_IUN/legal-facts",
      httpMethod: "GET",
    };
    const context = {};

    const response = await versioning(event, context);

    expect(response.statusCode).to.equal(500);
  });

  it("statusCode 502 - invalid endpoint", async () => {

    const iunValue = "12345";

    const event = {
      pathParameters: { iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: {},
      path: "/delivery/MOCK_IUN/legal-facts", // wrong
      httpMethod: "GET", // correct
    };
    const context = {};

    const response = await versioning(event, context);
    expect(response.statusCode).to.equal(502);

    event.path = "/delivery-push/MOCK_IUN/legal-facts"; // correct
    event.httpMethod = "POST"; // wrong
    const response2 = await versioning(event, context);
    expect(response2.statusCode).to.equal(502);
  });

  it("fetch throw error", async () => {

    process.env = Object.assign(process.env, {
      PN_DELIVERYPUSH_URL: "https://api.dev.notifichedigitali.it/delivery-push",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERYPUSH_URL}/${iunValue}/legal-facts`;

    mock.onGet(url).abortRequest();

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: `/delivery-push/${iunValue}/legal-facts`,
      path: "/delivery-push/MOCK_IUN/legal-facts",
      httpMethod: "GET",
    };
    const context = {};

    const response = await versioning(event, context);
    console.log("response ", response)

    expect(response.statusCode).to.equal(500);
  });

  it("statusCode 200 headers setting", async () => {
    const responseJSON = fs.readFileSync("./src/test/response.json");
    let responseObj = JSON.parse(responseJSON);

    process.env = Object.assign(process.env, {
      PN_DELIVERYPUSH_URL: "https://api.dev.notifichedigitali.it/delivery-push",
    });

    const iunValue = "12345";

    let url = `${process.env.PN_DELIVERYPUSH_URL}/${iunValue}/legal-facts`;

    mock.onGet(url).reply(200, responseObj, { "Content-Type": "application/json" });

    const event = {
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {
          cx_groups: "aaa",
          cx_id: "bbb",
          cx_role: "ccc",
          cx_type: "ddd",
          cx_jti: "eee",
          sourceChannelDetails: "fff",
          uid: "ggg",
        },
      },
      resource: `/delivery-push/${iunValue}/legal-facts`,
      path: "/delivery-push/MOCK_IUN/legal-facts",
      httpMethod: "GET",
    };

    const context = {};

    const response = await versioning(event, context);

    expect(response.statusCode).to.equal(200);
  });

  describe("axios-retry configurato una sola volta", function () {
    const iunValue = "12345";
    const EXPECTED_CALLS_ON_FAILURE = 1 + Number(process.env.NUM_RETRY); // tentativo iniziale + retry

    const buildEvent = () => ({
      pathParameters: { iun: iunValue },
      headers: {},
      requestContext: {
        authorizer: {},
      },
      resource: `/delivery-push/${iunValue}/legal-facts`,
      path: "/delivery-push/MOCK_IUN/legal-facts",
      httpMethod: "GET",
    });

    let url;

    beforeEach(() => {
      process.env = Object.assign(process.env, {
        PN_DELIVERYPUSH_URL: "https://api.dev.notifichedigitali.it/delivery-push",
      });
      url = `${process.env.PN_DELIVERYPUSH_URL}/${iunValue}/legal-facts`;
      mock.reset();
    });

    after(() => {
      mock.reset();
    });

    it("constant number of attempts across successive invocations", async () => {
      mock.onGet(url).reply(500, { error: "ERROR" });

      for (let i = 0; i < 3; i++) {
        mock.resetHistory();
        const response = await versioning(buildEvent(), {});
        expect(response.statusCode).to.equal(500);
        expect(mock.history.get.length).to.equal(EXPECTED_CALLS_ON_FAILURE);
      }
    });

    it("axios interceptors do not increase with each invocation", async () => {
      mock.onGet(url).reply(200, {});

      await versioning(buildEvent(), {});
      const requestHandlers = axios.interceptors.request.handlers.length;
      const responseHandlers = axios.interceptors.response.handlers.length;

      await versioning(buildEvent(), {});
      await versioning(buildEvent(), {});

      expect(axios.interceptors.request.handlers.length).to.equal(requestHandlers);
      expect(axios.interceptors.response.handlers.length).to.equal(responseHandlers);
    });
  });

});