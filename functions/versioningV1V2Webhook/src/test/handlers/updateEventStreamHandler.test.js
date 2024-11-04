const { expect } = require("chai");
const axios = require("axios");
const UpdateEventStreamHandler  = require("../../app/handlers/updateEventStreamHandler.js");

let MockAdapter = require("axios-mock-adapter")
let mock = new MockAdapter(axios);

describe("UpdateEventStreamHandler", () => {

    let updateEventStreamHandler;

    beforeEach(() => {
        updateEventStreamHandler = new UpdateEventStreamHandler();
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
                httpMethod: "PUT" };
            const result = updateEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("invalid ownership - case wrong method", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams",
                pathParameters : { streamId: streamId },
                httpMethod: "GET" };
            const result = updateEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case undefined", () => {
            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "PUT" };
            const result = updateEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case null", () => {
            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "PUT",
                pathParameters: null
            };
            const result = updateEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });
    });

    describe("handlerEvent", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.3",
        });

        it("successful request", async () => {
            const streamId = "12345";
            const b = JSON.stringify({
                                          title: "stream name",
                                          eventType: "STATUS",
                                          filterValues: ["status_1", "status_2"]
                                      });
            const event = {
                path: "/delivery-progresses/streams",
                pathParameters : { streamId: streamId },
                httpMethod: "PUT",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
                body: b
            };

            let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}`;

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

            const responseBodyV10 = {
                title: "stream name",
                eventType: "STATUS",
                filterValues: ["status_1", "status_2"],
                streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                activationDate: "2024-02-01T12:00:00Z"
            }

            mock.onPut(url).reply(200, responseBodyV23);

            const context = {};
            const response = await updateEventStreamHandler.handlerEvent(event, context);

            expect(response.statusCode).to.equal(200);
            expect(response.body).to.equal(JSON.stringify(responseBodyV10));

            expect(mock.history.put.length).to.equal(1);
        });
    });

    describe("handlerEvent", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.3",
        });

        it("successful request", async () => {
            const streamId = "12345";
            const b = JSON.stringify({
                                          title: "stream name",
                                          eventType: "STATUS",
                                          filterValues: ["status_1", "status_2"]
                                      });
            const event = {
                path: "/delivery-progresses/v2.4/streams",
                pathParameters : { streamId: streamId },
                httpMethod: "PUT",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
                body: b
            };

            let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}`;

            const responseBodyV24 = {
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
                version: "v24"
            }

            mock.onPut(url).reply(200, responseBodyV24);

            const context = {};
            const response = await updateEventStreamHandler.handlerEvent(event, context);

            expect(response.statusCode).to.equal(200);
            expect(response.body).to.equal(JSON.stringify(responseBodyV24));

            expect(mock.history.put.length).to.equal(1);
        });
    });

});