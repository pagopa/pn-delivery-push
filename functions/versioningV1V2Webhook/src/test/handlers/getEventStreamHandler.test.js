const { expect } = require("chai");
const axios = require("axios");
const GetEventStreamHandler = require("../../app/handlers/getEventStreamHandler")

let MockAdapter = require("axios-mock-adapter")
let mock = new MockAdapter(axios);

describe("GetEventStreamHandler", () => {

    let getEventStreamHandler;

    beforeEach(() => {
        getEventStreamHandler = new GetEventStreamHandler();
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
                httpMethod: "GET"
            };
            const result = getEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("invalid ownership - case 1", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams",
                pathParameters : { streamId: streamId },
                httpMethod: "POST"
            };
            const result = getEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case 2", () => {
            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "GET"
            };
            const result = getEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case 3", () => {
            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "GET",
                pathParameters: null
            };
            const result = getEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });
    });

    describe("handlerEvent that applies a map function for response body", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.7",
        });

        it("successful request", async () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams/" + streamId,
                pathParameters : { streamId: streamId },
                httpMethod: "GET",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
                body: {
                    title: "stream name",
                    eventType: "STATUS",
                    filterValues: ["status_1", "status_2"]
                }
            };

            let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}`;

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
                version: "v10"
            }

            const responseBodyV10 = {
                title: "stream name",
                eventType: "STATUS",
                filterValues: ["status_1", "status_2"],
                streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                activationDate: "2024-02-01T12:00:00Z"
            }

            mock.onGet(url).reply(200, responseBodyV26);

            const context = {};
            const response = await getEventStreamHandler.handlerEvent(event, context);

            expect(response.statusCode).to.equal(200);
            expect(response.body).to.equal(JSON.stringify(responseBodyV10));
            expect(mock.history.get.length).to.equal(1);
        });
    });

    describe("handlerEvent that does not apply a map function for response body", () => {

        let getEventStreamHandler;

        beforeEach(() => {
            getEventStreamHandler = new GetEventStreamHandler();
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
                    groups: [
                        { groupId: "group1", groupName: "Group One" },
                        { groupId: "group2", groupName: "Group Two" }
                    ],
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    disabledDate: "2024-02-02T12:00:00Z",
                    waitForAccepted: true,
                    version: "v10"
                }
            },
            {
                version: "v2.3",
                responseBody: {
                    title: "stream name",
                    eventType: "STATUS",
                    groups: [
                        { groupId: "group1", groupName: "Group One" },
                        { groupId: "group2", groupName: "Group Two" }
                    ],
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    disabledDate: "2024-02-02T12:00:00Z",
                    waitForAccepted: true,
                    version: "v23"
                }
            },
            {
                version: "v2.4",
                responseBody: {
                    title: "stream name",
                    eventType: "STATUS",
                    groups: [
                        { groupId: "group1", groupName: "Group One" },
                        { groupId: "group2", groupName: "Group Two" }
                    ],
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    disabledDate: "2024-02-02T12:00:00Z",
                    waitForAccepted: true,
                    version: "v24"
                }
            },
            {
                version: "v2.5",
                responseBody: {
                    title: "stream name",
                    eventType: "STATUS",
                    groups: [
                        { groupId: "group1", groupName: "Group One" },
                        { groupId: "group2", groupName: "Group Two" }
                    ],
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    disabledDate: "2024-02-02T12:00:00Z",
                    waitForAccepted: true,
                    version: "v25"
                }
            },
            {
                version: "v2.6",
                responseBody: {
                    title: "stream name",
                    eventType: "STATUS",
                    groups: [
                        { groupId: "group1", groupName: "Group One" },
                        { groupId: "group2", groupName: "Group Two" }
                    ],
                    filterValues: ["status_1", "status_2"],
                    streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv",
                    activationDate: "2024-02-01T12:00:00Z",
                    disabledDate: "2024-02-02T12:00:00Z",
                    waitForAccepted: true,
                    version: "v26"
                }
            }
        ];

        describe("handlerEvent", () => {

            process.env = Object.assign(process.env, {
                PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.7",
            });

            testCases.forEach(({ version, responseBody }) => {
                it(`successful request ${version}`, async () => {
                    const streamId = "12345";
                    const event = {
                        path: `/delivery-progresses/${version}/streams/${streamId}`,
                        pathParameters: { streamId: streamId },
                        httpMethod: "GET",
                        headers: {},
                        requestContext: {
                            authorizer: {},
                        },
                        body: {
                            title: "stream name",
                            eventType: "STATUS",
                            filterValues: ["status_1", "status_2"]
                        }
                    };

                    let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}`;

                    mock.onGet(url).reply(200, responseBody);

                    const context = {};
                    const response = await getEventStreamHandler.handlerEvent(event, context);

                    expect(response.statusCode).to.equal(200);
                    console.log(response.body)
                    expect(response.body.waitForAccepted).to.be.undefined
                    expect(mock.history.get.length).to.equal(1);
                });
            });
        });
    });
});