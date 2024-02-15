const { expect } = require("chai");
const EventHandler = require("../../app/handlers/baseHandler");

describe('EventHandler', () => {
    let eventHandler;

    beforeEach(() => {
        eventHandler = new EventHandler();
    });

    process.env = Object.assign(process.env, {
        PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.3",
    });

    it('correct URL', () => {
        expect(eventHandler.baseUrl).to.equal("https://api.dev.notifichedigitali.it/delivery-progresses/v2.3");
    });

    it('prepare headers correctly', () => {
        const streamId = "12345";

        const event = {
            pathParameters: { streamId: streamId },
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
            httpMethod: "GET",
        };

        const context = {};
        const headers = eventHandler.prepareHeaders(event, context);

        const headersToCompare = {
            "x-pagopa-pn-src-ch": "B2B",
            "x-pagopa-pn-cx-groups": "aaa",
            "x-pagopa-pn-cx-id": "bbb",
            "x-pagopa-pn-cx-role": "ccc",
            "x-pagopa-pn-cx-type": "ddd",
            "x-pagopa-pn-jti": "eee",
            "x-pagopa-pn-src-ch-details": "fff",
            "x-pagopa-pn-uid": "ggg",
            'x-pagopa-pn-api-version': 'v10'
        };

        expect(headers).to.deep.equal(headersToCompare);
    })
});