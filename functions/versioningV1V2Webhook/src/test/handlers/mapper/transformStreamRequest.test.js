const { expect } = require('chai');
const { createStreamRequestV22 } = require('../../../app/handlers/mapper/transformStreamRequestFromV10ToV23.js');

describe('createStreamRequestV22', () => {
    it('create a stream request object with the correct properties', () => {

        const requestBody = {
            title: 'Test Stream',
            eventType: 'testEventType',
            filterValues: ['filter1', 'filter2']
        };

        const result = createStreamRequestV22(requestBody);

        expect(result.title).to.equal(requestBody.title);
        expect(result.eventType).to.equal(requestBody.eventType);
        expect(result.groups).to.be.an('array').that.is.empty;
        expect(result.filterValues).to.deep.equal(requestBody.filterValues);
    });

});
