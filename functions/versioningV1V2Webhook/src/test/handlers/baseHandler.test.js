const { expect } = require("chai");
const EventHandler = require("../../app/handlers/baseHandler");

describe('EventHandler', () => {
    let eventHandler;

    beforeEach(() => {
        eventHandler = new EventHandler();
    });

    process.env = Object.assign(process.env, {
        PN_DELIVERY_PUSH_URL: "https://api.dev.notifichedigitali.it",
    });

    it('correct URL', () => {
        expect(eventHandler.url).to.equal("https://api.dev.notifichedigitali.it");
    });

    it('set headers correctly - case 1', () => {
        const event = {
            headers: {
                'x-pagopa-pn-uid': 'uid',
                'x-pagopa-pn-cx-type': 'type',
                'x-pagopa-pn-cx-id': 'cx-id',
                'streamId': 'streamId',
                'lastEventId': 'lastEventId',
            },
        };

        const headers = eventHandler.setHeaders(event, {});

        expect(headers).to.deep.equal({
            'x-pagopa-pn-uid': 'uid',
            'x-pagopa-pn-cx-type': 'type',
            'x-pagopa-pn-cx-id': 'cx-id',
            'streamId': 'streamId',
            'lastEventId': 'lastEventId',
            'x-pagopa-pn-cx-groups': null,
            'x-pagopa-pn-api-version': 'v10',
        });
    })

    it('set headers correctly - case 2', () => {
        const event = {
            headers: {},
        };

        const headers = eventHandler.setHeaders(event, {});

        expect(headers).to.deep.equal({
            'x-pagopa-pn-cx-groups': null,
            'x-pagopa-pn-api-version': 'v10',
        });
    });
});