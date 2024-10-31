const { expect } = require('chai');
const { createProgressResponseV24 } = require('../../../app/handlers/mapper/transformProgressResponseFromV25ToV24');
const fs = require("fs");

describe('createProgressResponseV24', () => {
    it('checkProgressResponseV24"', () => {

        const progressResponse = fs.readFileSync("./src/test/handlers/mapper/progressResponseV25.json");
        let responseBody = JSON.parse(progressResponse);

        const result = createProgressResponseV24(responseBody);
        expect(result).to.be.not.null;
        console.log('result is ', JSON.stringify(result))

        expect(result.element.legalFactsIds).to.be.empty;
    });
});