const { expect } = require('chai');
const { createStreamMetadataResponseV10 } = require('../../../app/handlers/mapper/transformStreamMetadataResponseFromV23ToV10.js');

describe('createStreamMetadataResponseV10', () => {
    it('should create a stream metadata response object with the correct properties', () => {

        const responseBodyV23 = {
            title: "stream name",
            eventType: "STATUS",
            groups: [{
                groupId: "group1",
                groupName: "Group One"
            },
                {
                    groupId: "group2",
                    groupName: "Group Two"
                }],
            filterValues: ["status_1", "status_2"],
            streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
            activationDate: "2024-02-01T12:00:00Z",
            disabledDate: "2024-02-02T12:00:00Z",
            version: "v10"
        }

        const result = createStreamMetadataResponseV10(responseBodyV23);

        expect(result).to.be.an('object');
        expect(result.title).to.equal(responseBodyV23.title);
        expect(result.eventType).to.equal(responseBodyV23.eventType);
        expect(result.filterValues).to.deep.equal(responseBodyV23.filterValues);
        expect(result.streamId).to.equal(responseBodyV23.streamId);
        expect(result.activationDate).to.equal(responseBodyV23.activationDate);

        expect(result).to.not.have.property('disabledDate');
        expect(result).to.not.have.property('version');
    });
});
