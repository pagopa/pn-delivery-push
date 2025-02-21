const { expect } = require("chai");
const axios = require("axios");
const DisableEventStreamHandler  = require("../../app/handlers/disableEventStreamHandler.js");

let MockAdapter = require("axios-mock-adapter")
let mock = new MockAdapter(axios);

describe("DisableEventStreamHandler", () => {

    let disableEventStreamHandler;

    beforeEach(() => {
        disableEventStreamHandler = new DisableEventStreamHandler();
        mock = new MockAdapter(axios);
    });

    afterEach(() => {
        mock.restore();
    });

    describe("checkOwnership", () => {
        it("valid ownership", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/v2.3/streams/{streamId}/action/disable",
                pathParameters : { streamId: streamId },
                httpMethod: "POST" };
            const result = disableEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("invalid ownership - case wrong method", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/v2.3/streams/{streamId}/action/disable",
                pathParameters : { streamId: streamId },
                httpMethod: "GET" };
            const result = disableEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case undefined", () => {
            const event = {
                path: "/delivery-progresses/v2.3/streams/{streamId}/action/disable",
                httpMethod: "PUT" };
            const result = disableEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case null", () => {
            const event = {
                path: "/delivery-progresses/v2.3/streams/{streamId}/action/disable   ",
                httpMethod: "PUT",
                pathParameters: null
            };
            const result = disableEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });
    });

    describe("handlerEvent", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.4",
        });

        it("successful request", async () => {
            const streamId = "12345";
            const b = '{}'
            const event = {
                path: "/delivery-progresses/v2.3/streams/{streamId}/action/disable",
                pathParameters : { streamId: streamId },
                httpMethod: "PUT",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
                body: b
            };

            let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/action/disable`;

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

            mock.onPost(url).reply(200, responseBodyV23);

            const context = {};
            const response = await disableEventStreamHandler.handlerEvent(event, context);

            expect(response.statusCode).to.equal(200);
            expect(response.body).to.equal(JSON.stringify(responseBodyV23));

            expect(mock.history.post.length).to.equal(1);
        });    
    });

    describe("handlerEvent", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.6",
        });

        it("successful request 2.5", async () => {
            const streamId = "12345";
            const b = '{}'
            const event = {
                path: "/delivery-progresses/v2.5/streams/{streamId}/action/disable",
                pathParameters : { streamId: streamId },
                httpMethod: "PUT",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
                body: b
            };

            let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/action/disable`;

            const responseBodyV25 = {
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

            mock.onPost(url).reply(200, responseBodyV25);

            const context = {};
            const response = await disableEventStreamHandler.handlerEvent(event, context);

            expect(response.statusCode).to.equal(200);
            expect(response.body).to.equal(JSON.stringify(responseBodyV25));

            expect(mock.history.post.length).to.equal(1);
        });    
    });

    describe("handlerEvent", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.6",
        });

        it("successful request 2.4", async () => {
            const streamId = "12345";
            const b = '{}'
            const event = {
                path: "/delivery-progresses/v2.4/streams/{streamId}/action/disable",
                pathParameters : { streamId: streamId },
                httpMethod: "PUT",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
                body: b
            };

            let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/action/disable`;

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

            mock.onPost(url).reply(200, responseBodyV24);

            const context = {};
            const response = await disableEventStreamHandler.handlerEvent(event, context);

            expect(response.statusCode).to.equal(200);
            expect(response.body).to.equal(JSON.stringify(responseBodyV24));

            expect(mock.history.post.length).to.equal(1);
        });    
    });

    describe("handlerEvent", () => {

        it("successful request 2.6", async () => {

            process.env = Object.assign(process.env, {
                PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.6",
            });

            disableEventStreamHandler = new DisableEventStreamHandler();

            const streamId = "12345";
            const b = '{}'
            const event = {
                path: "/delivery-progresses/v2.6/streams/{streamId}/action/disable",
                pathParameters : { streamId: streamId },
                httpMethod: "PUT",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
                body: b
            };

            let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/action/disable`;

            const responseBodyV26 = {
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
                version: "v26"
            }

            mock.onPost(url).reply(200, responseBodyV26);

            const context = {};
            const response = await disableEventStreamHandler.handlerEvent(event, context);

            expect(response.statusCode).to.equal(200);
            expect(response.body).to.equal(JSON.stringify(responseBodyV26));

            expect(mock.history.post.length).to.equal(1);
        });
    });

    describe("handlerEvent 1.0 error", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.4",
        });

        it("successful request", async () => {
            const streamId = "12345";
            const b = '{}'
            const event = {
                path: "/delivery-progresses/streams/{streamId}/action/disable",
                pathParameters : { streamId: streamId },
                httpMethod: "PUT",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
                body: b
            };

            const context = {};
            
            try {
                await disableEventStreamHandler.handlerEvent(event, context);
            } catch (error) {
                expect(error.message).to.equal('NOT IMPLEMENTED');
            }
        });
    });

});