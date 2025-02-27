const { expect } = require("chai");
const axios = require("axios");
const CreateEventStreamHandler  = require("../../app/handlers/createEventStreamHandler.js");

let MockAdapter = require("axios-mock-adapter")
let mock = new MockAdapter(axios);
describe("CreateEventStreamHandler", () => {

    let createEventStreamHandler;

    beforeEach(() => {
        createEventStreamHandler = new CreateEventStreamHandler();
        mock = new MockAdapter(axios);
    });

    afterEach(() => {
        mock.restore();
    });

    describe("checkOwnership", () => {
        it("valid ownership - case 1", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams",
                pathParameters : { streamId: streamId },
                httpMethod: "POST" };
            const result = createEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("valid ownership - case 2", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams/",
                pathParameters : { streamId: streamId },
                httpMethod: "POST" };
            const result = createEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("invalid ownership - case 1", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams",
                pathParameters : { streamId: streamId },
                httpMethod: "GET" };
            const result = createEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case 2", () => {
            const event = {
                path: "/delivery-progresses/streams/{streamId}",
                httpMethod: "POST" };
            const result = createEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });
    });

    describe("handlerEvent that applies a map function for response body", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.7",
        });

        it("successful request v10", async () => {

            const b = JSON.stringify({
                                          title: "stream name",
                                          eventType: "STATUS",
                                          filterValues: ["status_1", "status_2"],
                                          waitForAccepted: true
                                      });

            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "POST",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
                body : b
            };

            let url = `${process.env.PN_WEBHOOK_URL}/streams`

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

            mock.onPost(url).reply(200, responseBodyV23);

            const context = {};
            const response = await createEventStreamHandler.handlerEvent(event, context);

            expect(response.statusCode).to.equal(200);
            expect(response.body).to.equal(JSON.stringify(responseBodyV10));

            expect(mock.history.post.length).to.equal(1);
        });
    });

    describe("handlerEvent that applies a map function for waitForAccept in response and request body", () => {

        let createEventStreamHandler;

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.7",
        });

        beforeEach(() => {
            createEventStreamHandler = new CreateEventStreamHandler();
            mock = new MockAdapter(axios);
        });

        afterEach(() => {
            mock.restore();
        });

        const testCases = [
            {
                version: "v1.0",
                responseBody: {
                    title: "stream name",
                    eventType: "STATUS",
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    waitForAccepted: false
                }
            },
            {
                version: "v2.3",
                responseBody: {
                    title: "stream name",
                    eventType: "STATUS",
                    groups: [],
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    disabledDate: "2024-02-02T12:00:00Z",
                    version: "v23",
                    waitForAccepted: false
                }
            },
            {
                version: "v2.4",
                responseBody: {
                    title: "stream name",
                    eventType: "STATUS",
                    groups: [],
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    version: "v24",
                    waitForAccepted: false
                }
            },
            {
                version: "v2.5",
                responseBody: {
                    title: "stream name",
                    eventType: "STATUS",
                    groups: [],
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    version: "v25",
                    waitForAccepted: false
                }
            },
            {
                version: "v2.6",
                responseBody: {
                    title: "stream name",
                    eventType: "STATUS",
                    groups: [],
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    version: "v26",
                    waitForAccepted: false
                }
            }]

             testCases.forEach(({ version, responseBody }) => {
                            it(`successful request ${version}`, async () => {
                                const b = JSON.stringify({
                                    title: "stream name",
                                    eventType: "STATUS",
                                    filterValues: ["status_1", "status_2"],
                                    waitForAccepted: true
                                });

                                const event = {
                                    path: `/delivery-progresses/${version}/streams`,
                                    httpMethod: "POST",
                                    headers: {},
                                    requestContext: {
                                        authorizer: {},
                                    },
                                    body: b
                                };

                                let url = `${process.env.PN_WEBHOOK_URL}/streams`;
                                
                                 mock.onPost(url).reply((config) => {
                                    capturedRequestBody = JSON.parse(config.data); 
                                    expect(capturedRequestBody.waitForAccepted).to.be.undefined
                                    return [200, responseBody];
                                  });
                               

                                const context = {};
                                const response = await createEventStreamHandler.handlerEvent(event, context);

                                expect(response.statusCode).to.equal(200);
                                expect(mock.history.post.length).to.equal(1);;
                                console.log(response.body)
                                expect(response.body.waitForAccepted).to.be.undefined
                            });
                        });

    });
});