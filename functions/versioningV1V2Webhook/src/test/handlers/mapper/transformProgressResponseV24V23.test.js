const { expect } = require('chai');
const { createProgressResponseV23 } = require('../../../app/handlers/mapper/transformProgressResponseFromV24ToV23');
const fs = require("fs");

describe('createProgressResponseV23', () => {
    it('checkProgressResponseV23"', () => {

        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponseV24.json");
        let responseBody = JSON.parse(progressResponse);

        const result = createProgressResponseV23(responseBody);
        expect(result).to.be.not.null;
        console.log('result is ', JSON.stringify(result))

        expect(result.element.ingestionTimestamp).to.equal(undefined);
        expect(result.element.notificationSentAt).to.equal(undefined);
    });
});