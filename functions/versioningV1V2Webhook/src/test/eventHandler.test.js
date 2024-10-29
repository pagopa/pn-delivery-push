const { expect } = require('chai');
const { eventHandler } = require("../app/eventHandler.js")
const EventHandler = require("../app/handlers/baseHandler");
const proxyquire = require("proxyquire").noPreserveCache();
const streamId = 18;

describe('handleEvent', function () {

    it("call consumeEventStreamHandler when checkOwnership returns true", async () => {

        const lambda = proxyquire("../app/eventHandler.js", {
            "./handlers/consumeEventStreamHandler.js":
                class MockConsumeEventStreamHandler {
                        checkOwnership = () => true;
                        handlerEvent = () => ({
                            statusCode: 200,
                            body: 'A'
                        });
                },
            "../app/handlers/createEventStreamHandler":
                class MockCreateEventStreamHandler {
                    checkOwnership = () => false;
                    handlerEvent = () => ({
                        statusCode: 500,
                        body: 'B'
                    });
                }
        });

        const event = {
            path: "/delivery-progresses/v2.3/streams/"+ streamId +"/events",
            pathParameters : { streamId: streamId },
            queryStringParameters: null,
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
        }

        const result = await lambda.eventHandler(event, {});

        expect(result.statusCode).to.equal(200);
        expect(result.body).to.equal('A');

    });

    it("call createEventStreamHandler when checkOwnership returns true", async () => {

        const lambda = proxyquire("../app/eventHandler.js", {
            "./handlers/consumeEventStreamHandler.js":
                class MockConsumeEventStreamHandler {
                    checkOwnership = () => false;
                    handlerEvent = () => ({
                        statusCode: 500,
                        body: 'A'
                    });
                },
            "../app/handlers/createEventStreamHandler":
                class MockCreateEventStreamHandler {
                    checkOwnership = () => true;
                    handlerEvent = () => ({
                        statusCode: 200,
                        body: 'B'
                    });
                }
        });
        
        const event = {
            path: "/delivery-progresses/v2.3/streams/"+ streamId +"/events",
            pathParameters : { streamId: streamId },
            queryStringParameters: null,
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
        }

        const result = await lambda.eventHandler(event, {});

        expect(result.statusCode).to.equal(200);
        expect(result.body).to.equal('B');

    });

    it("catch and handle error in handlerEvent", async () => {

        const lambda = proxyquire("../app/eventHandler.js", {
            "../app/handlers/createEventStreamHandler":
                class MockCreateEventStreamHandler {
                    checkOwnership = () => true;
                    handlerEvent = () => {
                        throw new Error("Errore Simulato");
                    };

                }
        });
        
        const result = await lambda.eventHandler({}, {});

        expect(result.statusCode).to.equal(500);
        expect(result.body).to.match(/(?:PN_GENERIC_ERROR)/);
    });

    it("statusCode 403 to 403", async () => {

        const lambda = proxyquire("../app/eventHandler.js", {
            "../app/handlers/consumeEventStreamHandler":
                class MockConsumeEventStreamHandler {
                    checkOwnership = () => true;
                    handlerEvent = () => {
                        let error = new Error();
                        error.response = {
                            status: 403,
                        };
                        throw error;
                    };
                }
        });

        const event = {
            path: "/delivery-progresses/v2.3/streams/"+ streamId +"/events",
            pathParameters : { streamId: streamId },
            queryStringParameters: null,
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
        }

        const result = await lambda.eventHandler(event, {});

        expect(result.statusCode).to.equal(403);
    });

    it("statusCode 404 to 404", async () => {

        const lambda = proxyquire("../app/eventHandler.js", {
            "../app/handlers/consumeEventStreamHandler":
                class MockConsumeEventStreamHandler {
                    checkOwnership = () => true;
                    handlerEvent = () => {
                        let error = new Error();
                        error.response = {
                            status: 404
                        };
                        throw error;
                    };
                }
        });
        
        const event = {
            path: "/delivery-progresses/v2.3/streams/"+ streamId +"/events",
            pathParameters : { streamId: streamId },
            queryStringParameters: null,
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
        }

        const result = await lambda.eventHandler(event, {});

        expect(result.statusCode).to.equal(404);
    });


    it("statusCode 403 to 400", async () => {

        const lambda = proxyquire("../app/eventHandler.js", {
            "../app/handlers/consumeEventStreamHandler":
                class MockConsumeEventStreamHandler {
                    checkOwnership = () => true;
                    handlerEvent = () => {
                        let error = new Error();
                        error.response = {
                            status: 403,
                        };
                        throw error;
                    };
                }
        });

        const event = {
            path: "/delivery-progresses/streams/"+ streamId +"/events",
            pathParameters : { streamId: streamId },
            queryStringParameters: null,
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
        }

        const result = await lambda.eventHandler(event, {});

        expect(result.statusCode).to.equal(400);
    });

    it("statusCode 404 to 400", async () => {

        const lambda = proxyquire("../app/eventHandler.js", {
            "../app/handlers/consumeEventStreamHandler":
                class MockConsumeEventStreamHandler {
                    checkOwnership = () => true;
                    handlerEvent = () => {
                        let error = new Error();
                        error.response = {
                            status: 404
                        };
                        throw error;
                    };
                }
        });
        
        const event = {
            path: "/delivery-progresses/streams/"+ streamId +"/events",
            pathParameters : { streamId: streamId },
            queryStringParameters: null,
            httpMethod: "GET",
            headers: {},
            requestContext: {
                authorizer: {},
            },
        }

        const result = await lambda.eventHandler(event, {});

        expect(result.statusCode).to.equal(400);
    });

    it("error endpoint", async () => {

        const event = {
            path: "/delivery-progresses",
            httpMethod: "GET"
        }

        const result = await eventHandler(event, {});

        expect(result.statusCode).to.equal(500);
        expect(result.body).to.match(/(?:ENDPOINT ERRATO)/);
    });

});