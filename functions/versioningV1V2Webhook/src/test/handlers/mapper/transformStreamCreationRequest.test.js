const { expect } = require('chai');
const { createStreamCreationRequestV22 } = require('../../../app/handlers/mapper/transformStreamCreationRequestFromV10ToV23.js');

describe('createStreamCreationRequestV22', () => {
    it('create a stream creation request object with the correct properties', () => {

        const requestBody = {
            title: 'Test Stream',
            eventType: 'testEventType',
            filterValues: ['filter1', 'filter2']
        };

        const result = createStreamCreationRequestV22(requestBody);

        expect(result.title).to.equal(requestBody.title);
        expect(result.eventType).to.equal(requestBody.eventType);
        expect(result.groups).to.be.null;
        expect(result.filterValues).to.deep.equal(requestBody.filterValues);
        expect(result.replacedStreamId).to.be.null;
    });

});