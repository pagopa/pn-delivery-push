const { expect } = require('chai');
const { createProgressResponseV10 } = require('../../../app/handlers/mapper/transformProgressResponseFromV23ToV10');
const fs = require("fs");

describe('createProgressResponseV10', () => {
    it('check category "SEND_ANALOG_DOMICILE"', () => {

        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponse.json");
        let responseBody = JSON.parse(progressResponse);

        responseBody.element = responseBody.element[0];

        const result = createProgressResponseV10(responseBody);

        expect(result.eventId).to.equal(responseBody.eventId);
        expect(result.notificationRequestId).to.equal(responseBody.notificationRequestId);
        expect(result.iun).to.equal(responseBody.iun);
        expect(result.newStatus).to.equal(responseBody.newStatus);

        expect(result.timestamp).to.equal(responseBody.element.timestamp);
        expect(result.timelineEventCategory).to.equal(responseBody.element.category);

        let legalFactsArray = [
            "example_document1.pdf",
            "example_document2.pdf"
        ];
        expect(JSON.stringify(result.legalFactsIds)).to.equal(JSON.stringify(legalFactsArray));

        expect(result.recipientIndex).to.equal(responseBody.element.details.recIndex);
        expect(result.analogCost).to.equal(responseBody.element.details.analogCost);
        expect(result.channel).to.equal(responseBody.element.details.serviceLevel);
        expect(result.validationErrors).to.null;

    });

    it('check category "REQUEST_REFUSED"', () => {
        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponse.json");
        let responseBody = JSON.parse(progressResponse);

        responseBody.element = responseBody.element[1];
        const result = createProgressResponseV10(responseBody);

        expect(JSON.stringify(result.validationErrors)).to.equal(JSON.stringify(responseBody.element.details.refusalReasons));
        expect(result.recipientIndex).to.equal(null);
    });
    it('check category "SIMPLE_REGISTERED_LETTER"', () => {
        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponse.json");
        let responseBody = JSON.parse(progressResponse);

        responseBody.element = responseBody.element[2];
        const result = createProgressResponseV10(responseBody);

        expect(result.recipientIndex).to.equal(responseBody.element.details.recIndex);
        expect(result.analogCost).to.equal(responseBody.element.details.analogCost);
        expect(result.channel).to.equal("SIMPLE_REGISTERED_LETTER");

    });
    it('check category "SEND_DIGITAL_DOMICILE"', () => {
        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponse.json");
        let responseBody = JSON.parse(progressResponse);

        responseBody.element = responseBody.element[3];
        const result = createProgressResponseV10(responseBody);

        expect(result.channel).to.equal("PEC");
    });
    it('check category "SEND_ANALOG_FEEDBACK"', () => {
        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponse.json");
        let responseBody = JSON.parse(progressResponse);

        responseBody.element = responseBody.element[4];
        const result = createProgressResponseV10(responseBody);

        expect(result.channel).to.equal(responseBody.element.details.serviceLevel);
    });
    it('check category "PREPARE_ANALOG_DOMICILE"', () => {
        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponse.json");
        let responseBody = JSON.parse(progressResponse);

        responseBody.element = responseBody.element[5];
        const result = createProgressResponseV10(responseBody);

        expect(result.channel).to.equal(responseBody.element.details.serviceLevel);
    });
    it('check category "PREPARE_SIMPLE_REGISTERED_LETTER"', () => {
        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponse.json");
        let responseBody = JSON.parse(progressResponse);

        responseBody.element = responseBody.element[6];
        const result = createProgressResponseV10(responseBody);

        expect(result.channel).to.equal("SIMPLE_REGISTERED_LETTER");
    });
    it('check category "SEND_DIGITAL_PROGRESS"', () => {
        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponse.json");
        let responseBody = JSON.parse(progressResponse);

        responseBody.element = responseBody.element[7];
        const result = createProgressResponseV10(responseBody);

        expect(result.channel).to.equal("PEC");
    });
    it('check category "SEND_DIGITAL_FEEDBACK"', () => {
        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponse.json");
        let responseBody = JSON.parse(progressResponse);

        responseBody.element = responseBody.element[8];
        const result = createProgressResponseV10(responseBody);

        expect(result.channel).to.equal("PEC");
    });

});