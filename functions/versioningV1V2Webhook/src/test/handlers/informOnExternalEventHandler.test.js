const { expect } = require("chai");
const axios = require("axios");
const InformOnExternalEventHandler  = require("../../app/handlers/informOnExternalEventHandler.js");

let MockAdapter = require("axios-mock-adapter")
let mock = new MockAdapter(axios);
describe("informOnExternalEventHandler", () => {
    let informOnExternalEventHandler;

    beforeEach(() => {
        informOnExternalEventHandler = new InformOnExternalEventHandler();
        mock = new MockAdapter(axios);
    });

    afterEach(() => {
        mock.restore();
    });

    describe("checkOwnership", () => {
        it("valid ownership", () => {
            const event = {
                path: "/delivery-progresses/events",
                httpMethod: "POST" };
            const result = informOnExternalEventHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("invalid ownership - case 1", () => {
            const event = {
                path: "/delivery-progresses/events",
                httpMethod: "GET" };
            const result = informOnExternalEventHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case 2", () => {
            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "POST" };
            const result = informOnExternalEventHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });
    });

    describe("handlerEvent", () => {

        it("throw exception : NOT YET IMPLEMENTED", async () => {
            try {
                await informOnExternalEventHandler.handlerEvent({}, {});
            } catch (error) {
                expect(error.message).to.equal('NOT YET IMPLEMENTED');
            }

        });
    });
});