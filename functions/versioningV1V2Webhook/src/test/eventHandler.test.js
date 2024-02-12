const { expect } = require('chai');
const { eventHandler } = require("../app/eventHandler.js")
const CreateEventStreamHandler = require("../app/handlers/createEventStreamHandler");
const UpdateEventStreamHandler = require("../app/handlers/updateEventStreamHandler");

describe('handleEvent', function () {

    it("call createStreamHandler when checkOwnership returns true", async () => {

        const createEventStreamHandlerInstanceA = new CreateEventStreamHandler();

        const originalCheckOwnershipA = createEventStreamHandlerInstanceA.checkOwnership;
        CreateEventStreamHandler.prototype.checkOwnership = () => {
            return true;
        };
        const originalHandlerEventA = createEventStreamHandlerInstanceA.handlerEvent;

        CreateEventStreamHandler.prototype.handlerEvent = () => ({
            statusCode: 200,
            body: 'A'
        });

        const updateEventStreamHandlerInstanceB = new UpdateEventStreamHandler();

        const originalCheckOwnershipB = updateEventStreamHandlerInstanceB.checkOwnership;
        UpdateEventStreamHandler.prototype.checkOwnership = () => {
            return false;
        };
        const originalHandlerEventB = updateEventStreamHandlerInstanceB.handlerEvent;
        UpdateEventStreamHandler.prototype.handlerEvent = () => ({
            statusCode: 200,
            body: 'B'
        });

        const result = await eventHandler({}, {});

        expect(result.statusCode).to.equal(200);
        expect(result.body).to.equal('A');

        CreateEventStreamHandler.prototype.checkOwnership = originalCheckOwnershipA;
        CreateEventStreamHandler.prototype.handlerEvent = originalHandlerEventA;

        UpdateEventStreamHandler.prototype.checkOwnership = originalCheckOwnershipB;
        UpdateEventStreamHandler.prototype.handlerEvent = originalHandlerEventB;

    });

    it("call upgradeStreamHandler when checkOwnership returns true", async () => {

        const createEventStreamHandlerInstanceA = new CreateEventStreamHandler();

        const originalCheckOwnershipA = createEventStreamHandlerInstanceA.checkOwnership;
        const originalHandlerEventA = createEventStreamHandlerInstanceA.handlerEvent;

        CreateEventStreamHandler.prototype.checkOwnership = () => {
            return false;
        };
        CreateEventStreamHandler.prototype.handlerEvent = () => ({
            statusCode: 200,
            body: 'A'
        });

        const updateEventStreamHandlerInstanceB = new UpdateEventStreamHandler();

        const originalCheckOwnershipB = updateEventStreamHandlerInstanceB.checkOwnership;
        const originalHandlerEventB = updateEventStreamHandlerInstanceB.handlerEvent;

        UpdateEventStreamHandler.prototype.checkOwnership = () => {
            return true;
        };
        UpdateEventStreamHandler.prototype.handlerEvent = () => ({
            statusCode: 404,
            body: 'B'
        });

        const result = await eventHandler({}, {});

        expect(result.statusCode).to.equal(400);
        expect(result.body).to.equal('B');

        CreateEventStreamHandler.prototype.checkOwnership = originalCheckOwnershipA;
        CreateEventStreamHandler.prototype.handlerEvent = originalHandlerEventA;

        UpdateEventStreamHandler.prototype.checkOwnership = originalCheckOwnershipB;
        UpdateEventStreamHandler.prototype.handlerEvent = originalHandlerEventB;
    });

    it("catch and handle error in handlerEvent", async () => {
        const createEventStreamHandlerInstance = new CreateEventStreamHandler();

        const originalHandlerEvent = createEventStreamHandlerInstance.handlerEvent;
        const originalCheckOwnership = createEventStreamHandlerInstance.checkOwnership;

        CreateEventStreamHandler.prototype.checkOwnership = () => {
            return true;
        };
        CreateEventStreamHandler.prototype.handlerEvent = () => {
            throw new Error("Errore Simulato")
        };

        const result = await eventHandler({}, {});

        expect(result.statusCode).to.equal(500);
        expect(result.body.errors[0].code).to.equal("PN_GENERIC_ERROR");

        CreateEventStreamHandler.prototype.checkOwnership = originalCheckOwnership;
        CreateEventStreamHandler.prototype.handlerEvent = originalHandlerEvent;
    });

    it("error endpoint", async () => {

        const event = {
            path: "/delivery-progresses/v2.3",
            httpMethod: "GET"
        }

        const result = await eventHandler(event, {});

        expect(result.statusCode).to.equal(500);
        expect(result.body.errors[0].code).to.equal("ENDPOINT ERRATO");
    });

});