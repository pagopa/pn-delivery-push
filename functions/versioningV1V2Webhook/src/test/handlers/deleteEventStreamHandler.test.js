const { expect } = require("chai");
const axios = require("axios");
const DeleteEventStreamHandler  = require("../../app/handlers/deleteEventStreamHandler.js");

let MockAdapter = require("axios-mock-adapter")
let mock = new MockAdapter(axios);

describe("DeleteEventStreamHandler", () => {

    let deleteEventStreamHandler;

    beforeEach(() => {
        deleteEventStreamHandler = new DeleteEventStreamHandler();
        mock = new MockAdapter(axios);
    });

    afterEach(() => {
        mock.restore();
    });

    describe("checkOwnership", () => {
        it("valid ownership", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams",
                pathParameters : { streamId: streamId },
                httpMethod: "DELETE"
            };
            const result = deleteEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("invalid ownership - case 1", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams",
                pathParameters : { streamId: streamId },
                httpMethod: "PUT"
            };
            const result = deleteEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case 2", () => {
            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "DELETE"
            };
            const result = deleteEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        describe("handlerEvent", () => {

            process.env = Object.assign(process.env, {
                PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.3",
            });

            it("successful request", async () => {
                const streamId = "12345";
                const event = {
                    path: "/delivery-progresses/streams",
                    pathParameters : { streamId: streamId },
                    httpMethod: "DELETE",
                    headers: {},
                    requestContext: {
                        authorizer: {},
                    },
                };

                let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}`;

                mock.onDelete(url).reply(204);

                const context = {};
                const response = await deleteEventStreamHandler.handlerEvent(event, context);

                expect(response.statusCode).to.equal(204);

                expect(mock.history.delete.length).to.equal(1);
            });
        });

    });
});