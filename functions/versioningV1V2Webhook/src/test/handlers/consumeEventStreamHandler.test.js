const { expect } = require("chai");
const axios = require("axios");
const ConsumeEventStreamHandler  = require("../../app/handlers/consumeEventStreamHandler");

let MockAdapter = require("axios-mock-adapter")
let mock = new MockAdapter(axios);
describe("ConsumeEventStreamHandler", () => {
    let consumeEventStreamHandler;

    beforeEach(() => {
        consumeEventStreamHandler = new ConsumeEventStreamHandler();
        mock = new MockAdapter(axios);
    });

    afterEach(() => {
        mock.restore();
    });

    describe("checkOwnership", () => {
        it("valid ownership - case 1", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams/"+ streamId +"/events",
                pathParameters : { streamId: streamId },
                httpMethod: "GET" };
            const result = consumeEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("valid ownership - case 2", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams/"+ streamId +"/events/",
                pathParameters : { streamId: streamId },
                httpMethod: "GET" };
            const result = consumeEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("invalid ownership - case 1", () => {
            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams/"+ streamId +"/events",
                pathParameters : { streamId: streamId },
                httpMethod: "POST" };
            const result = consumeEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case 2", () => {
            const event = {
                path: "/delivery-progresses/streams/",
                httpMethod: "GET" };
            const result = consumeEventStreamHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });
    });
    describe("handlerEvent", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.7",
            BASE_PATH: "http://localhost:9998"
        });

        it("successful request - with element", async () => {
                    const streamId = "12345";
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

                    let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events`;

                    const responseBodyV23 = [
                        {
                            eventId: "01234567890123456789012345678901234567",
                            notificationRequestId: "abcd1234",
                            iun: "ABCD-EFGH-IJKL-123456-M-7",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "abcdef1234567890",
                                timestamp: "2024-02-06T12:34:56Z",
                                legalFactsIds: [],
                                category: "SEND_COURTESY_MESSAGE",
                                details: {
                                    recIndex: 1,
                                    digitalAddress: {
                                        "type": "EMAIL",
                                        "address": "rec@example.com"
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: ""
                                },
                            }
                        },
                        {
                            eventId: "98765432109876543210987654321098765432",
                            notificationRequestId: "efgh5678",
                            iun: "EFGH-IJKL-MNOP-123456-N-8",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                legalFactsIds: [],
                                category: "SEND_DIGITAL_DOMICILE",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        }
                    ]

                    const responseBodyV10 = [
                        {
                            eventId: '01234567890123456789012345678901234567',
                            notificationRequestId: 'abcd1234',
                            iun: 'ABCD-EFGH-IJKL-123456-M-7',
                            newStatus: "IN_VALIDATION",
                            timestamp: '2024-02-06T12:34:56Z',
                            timelineEventCategory: 'SEND_COURTESY_MESSAGE',
                            recipientIndex: 1,
                            analogCost: null,
                            channel: 'EMAIL',
                            legalfactIds: [],
                            validationErrors: null
                        },
                        {
                            eventId: '98765432109876543210987654321098765432',
                            notificationRequestId: 'efgh5678',
                            iun: 'EFGH-IJKL-MNOP-123456-N-8',
                            newStatus: "IN_VALIDATION",
                            timestamp: '2024-02-07T14:45:32Z',
                            timelineEventCategory: 'SEND_DIGITAL_DOMICILE',
                            recipientIndex: 2,
                            analogCost: null,
                            channel: 'PEC',
                            legalfactIds: [],
                            validationErrors: null,
                        }
                    ]

                    mock.onGet(url).reply(200, responseBodyV23);

                    const context = {};
                    const response = await consumeEventStreamHandler.handlerEvent(event, context);

                    expect(response.statusCode).to.equal(200);
                    expect(response.body).to.equal(JSON.stringify(responseBodyV10));

                    expect(mock.history.get.length).to.equal(1);
                });

                it("successful request V24 to V23", async () => {
                    const streamId = "12345";
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

                    let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events`;

                    const responseBodyV24 = [
                        {
                            eventId: "01234567890123456789012345678901234567",
                            notificationRequestId: "abcd1234",
                            iun: "ABCD-EFGH-IJKL-123456-M-7",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "abcdef1234567890",
                                timestamp: "2024-02-06T12:34:56Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [],
                                category: "SEND_COURTESY_MESSAGE",
                                details: {
                                    recIndex: 1,
                                    digitalAddress: {
                                        "type": "EMAIL",
                                        "address": "rec@example.com"
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: ""
                                },
                            }
                        },
                        {
                            eventId: "98765432109876543210987654321098765432",
                            notificationRequestId: "efgh5678",
                            iun: "EFGH-IJKL-MNOP-123456-N-8",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [],
                                category: "SEND_DIGITAL_DOMICILE",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        }
                    ]

                    const responseBodyV23 = [
                        {
                            eventId: "01234567890123456789012345678901234567",
                            notificationRequestId: "abcd1234",
                            iun: "ABCD-EFGH-IJKL-123456-M-7",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "abcdef1234567890",
                                timestamp: "2024-02-06T12:34:56Z",
                                legalFactsIds: [],
                                category: "SEND_COURTESY_MESSAGE",
                                details: {
                                    recIndex: 1,
                                    digitalAddress: {
                                        "type": "EMAIL",
                                        "address": "rec@example.com"
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: ""
                                },
                            }
                        },
                        {
                            eventId: "98765432109876543210987654321098765432",
                            notificationRequestId: "efgh5678",
                            iun: "EFGH-IJKL-MNOP-123456-N-8",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                legalFactsIds: [],
                                category: "SEND_DIGITAL_DOMICILE",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        }
                    ]

                    mock.onGet(url).reply(200, responseBodyV24);

                    const context = {};
                    const response = await consumeEventStreamHandler.handlerEvent(event, context);

                    expect(response.statusCode).to.equal(200);
                    expect(response.body).to.equal(JSON.stringify(responseBodyV23));

                    expect(mock.history.get.length).to.equal(1);
                });

                it("successful request V24 to V10", async () => {
                    const streamId = "12345";
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

                    let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events`;

                    const responseBodyV24 = [
                        {
                            eventId: "01234567890123456789012345678901234567",
                            notificationRequestId: "abcd1234",
                            iun: "ABCD-EFGH-IJKL-123456-M-7",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "abcdef1234567890",
                                timestamp: "2024-02-06T12:34:56Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [],
                                category: "SEND_COURTESY_MESSAGE",
                                details: {
                                    recIndex: 1,
                                    digitalAddress: {
                                        "type": "EMAIL",
                                        "address": "rec@example.com"
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: ""
                                },
                            }
                        },
                        {
                            eventId: "98765432109876543210987654321098765432",
                            notificationRequestId: "efgh5678",
                            iun: "EFGH-IJKL-MNOP-123456-N-8",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [],
                                category: "SEND_DIGITAL_DOMICILE",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        },
                        {
                            eventId: "98765432109876543210987654321098765555",
                            notificationRequestId: "ilmn5678",
                            iun: "EFGH-IJKL-MNOP-123456-N-8",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [
                                    {
                                        key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                        category: "NOTIFICATION_CANCELLED"
                                    }
                                ],
                                category: "NOTIFICATION_CANCELLED",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        }
                    ]

                    const responseBodyV10 = [
                        {
                            eventId: '01234567890123456789012345678901234567',
                            notificationRequestId: 'abcd1234',
                            iun: 'ABCD-EFGH-IJKL-123456-M-7',
                            newStatus: "IN_VALIDATION",
                            timestamp: '2024-02-06T12:34:56Z',
                            timelineEventCategory: 'SEND_COURTESY_MESSAGE',
                            recipientIndex: 1,
                            analogCost: null,
                            channel: 'EMAIL',
                            legalfactIds: [],
                            validationErrors: null
                        },
                        {
                            eventId: '98765432109876543210987654321098765432',
                            notificationRequestId: 'efgh5678',
                            iun: 'EFGH-IJKL-MNOP-123456-N-8',
                            newStatus: "IN_VALIDATION",
                            timestamp: '2024-02-07T14:45:32Z',
                            timelineEventCategory: 'SEND_DIGITAL_DOMICILE',
                            recipientIndex: 2,
                            analogCost: null,
                            channel: 'PEC',
                            legalfactIds: [],
                            validationErrors: null
                        },
                        {
                            eventId: '98765432109876543210987654321098765555',
                            notificationRequestId: 'ilmn5678',
                            iun: 'EFGH-IJKL-MNOP-123456-N-8',
                            newStatus: "IN_VALIDATION",
                            timestamp: '2024-02-07T14:45:32Z',
                            timelineEventCategory: 'NOTIFICATION_CANCELLED',
                            recipientIndex: 2,
                            analogCost: null,
                            legalfactIds: [],
                            validationErrors: null
                        }
                    ]

                    mock.onGet(url).reply(200, responseBodyV24);

                    const context = {};
                    const response = await consumeEventStreamHandler.handlerEvent(event, context);

                    expect(response.statusCode).to.equal(200);
                    expect(response.body).to.equal(JSON.stringify(responseBodyV10));

                    expect(mock.history.get.length).to.equal(1);
                });

                it("successful request V25 to V24", async () => {
                    const streamId = "12345";
                    const event = {
                        path: "/delivery-progresses/v2.4/streams/"+ streamId +"/events",
                        pathParameters : { streamId: streamId },
                        queryStringParameters: null,
                        httpMethod: "GET",
                        headers: {},
                        requestContext: {
                            authorizer: {},
                        },
                    }

                    let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events`;

                    const responseBodyV25 = [
                        {
                            eventId: "01234567890123456789012345678901234567",
                            notificationRequestId: "abcd1234",
                            iun: "ABCD-EFGH-IJKL-123456-M-7",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "abcdef1234567890",
                                timestamp: "2024-02-06T12:34:56Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [
                                    {
                                        key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                        category: "NOTIFICATION_CANCELLED"
                                    }
                                ],
                                category: "NOTIFICATION_CANCELLED",
                                details: {
                                    recIndex: 1,
                                    digitalAddress: {
                                        type: "EMAIL",
                                        address: "rec@example.com"
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: ""
                                },
                            }
                        },
                        {
                            eventId: "98765432109876543210987654321098765432",
                            notificationRequestId: "efgh5678",
                            iun: "EFGH-IJKL-MNOP-123456-N-8",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [
                                    {
                                        key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                        category: "DIGITAL_DELIVERY"
                                    }
                                ],
                                category: "SEND_DIGITAL_DOMICILE",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        }
                    ]

                    const responseBodyV24 = [
                        {
                            eventId: "01234567890123456789012345678901234567",
                            notificationRequestId: "abcd1234",
                            iun: "ABCD-EFGH-IJKL-123456-M-7",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "abcdef1234567890",
                                timestamp: "2024-02-06T12:34:56Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [],
                                category: "NOTIFICATION_CANCELLED",
                                details: {
                                    recIndex: 1,
                                    digitalAddress: {
                                        "type": "EMAIL",
                                        "address": "rec@example.com"
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: ""
                                },
                            }
                        },
                        {
                            eventId: "98765432109876543210987654321098765432",
                            notificationRequestId: "efgh5678",
                            iun: "EFGH-IJKL-MNOP-123456-N-8",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [
                                    {
                                        key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                        category: "DIGITAL_DELIVERY"
                                    }
                                ],
                                category: "SEND_DIGITAL_DOMICILE",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        }
                    ]

                    mock.onGet(url).reply(200, responseBodyV25);

                    const context = {};
                    const response = await consumeEventStreamHandler.handlerEvent(event, context);

                    expect(response.statusCode).to.equal(200);
                    expect(response.body).to.equal(JSON.stringify(responseBodyV24));

                    expect(mock.history.get.length).to.equal(1);
                });

                it("successful request V26 to V25", async () => {
                    const streamId = "12345";
                    const event = {
                        path: "/delivery-progresses/v2.5/streams/"+ streamId +"/events",
                        pathParameters : { streamId: streamId },
                        queryStringParameters: null,
                        httpMethod: "GET",
                        headers: {},
                        requestContext: {
                            authorizer: {},
                        },
                    }

                    let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events`;

                    const responseBodyV26 = [
                        {
                            eventId: "01234567890123456789012345678901234567",
                            notificationRequestId: "abcd1234",
                            iun: "ABCD-EFGH-IJKL-123456-M-7",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "abcdef1234567890",
                                timestamp: "2024-02-06T12:34:56Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [
                                    {
                                        key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                        category: "NOTIFICATION_CANCELLED"
                                    }
                                ],
                                category: "NOTIFICATION_CANCELLED",
                                details: {
                                    recIndex: 1,
                                    digitalAddress: {
                                        type: "EMAIL",
                                        address: "rec@example.com"
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: ""
                                },
                            }
                        },
                        {
                            eventId: "98765432109876543210987654321098765432",
                            notificationRequestId: "efgh5678",
                            iun: "EFGH-IJKL-MNOP-123456-N-8",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [
                                    {
                                        key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                        category: "DIGITAL_DELIVERY"
                                    }
                                ],
                                category: "SEND_DIGITAL_DOMICILE",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        }
                    ]

                    mock.onGet(url).reply(200, responseBodyV26);

                    const context = {};
                    const response = await consumeEventStreamHandler.handlerEvent(event, context);

                    expect(response.statusCode).to.equal(200);
                    expect(response.body).to.equal(JSON.stringify(responseBodyV26));

                    expect(mock.history.get.length).to.equal(1);
                });

                it("successful request - with element", async () => {
                    const streamId = "12345";
                    const event = {
                        path: "/delivery-progresses/streams/"+ streamId +"/events",
                        pathParameters : { streamId: streamId },
                        queryStringParameters: { lastEventId: '00000000000000000000000000000000000083' },
                        multiValueQueryStringParameters: { lastEventId: [ '00000000000000000000000000000000000083' ] },
                        httpMethod: "GET",
                        headers: {},
                        requestContext: {
                            authorizer: {},
                        },
                    }

                    let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events?lastEventId=00000000000000000000000000000000000083`;

                    const responseBodyV23 = [
                        {
                            eventId: "01234567890123456789012345678901234567",
                            notificationRequestId: "abcd1234",
                            iun: "ABCD-EFGH-IJKL-123456-M-7",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "abcdef1234567890",
                                timestamp: "2024-02-06T12:34:56Z",
                                legalFactsIds: [],
                                category: "SEND_COURTESY_MESSAGE",
                                details: {
                                    recIndex: 1,
                                    digitalAddress: {
                                        "type": "EMAIL",
                                        "address": "rec@example.com"
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: ""
                                },
                            }
                        },
                        {
                            eventId: "98765432109876543210987654321098765432",
                            notificationRequestId: "efgh5678",
                            iun: "EFGH-IJKL-MNOP-123456-N-8",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                legalFactsIds: [],
                                category: "SEND_DIGITAL_DOMICILE",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        }
                    ]

                    const responseBodyV10 = [
                        {
                            eventId: '01234567890123456789012345678901234567',
                            notificationRequestId: 'abcd1234',
                            iun: 'ABCD-EFGH-IJKL-123456-M-7',
                            newStatus: "IN_VALIDATION",
                            timestamp: '2024-02-06T12:34:56Z',
                            timelineEventCategory: 'SEND_COURTESY_MESSAGE',
                            recipientIndex: 1,
                            analogCost: null,
                            channel: 'EMAIL',
                            legalfactIds: [],
                            validationErrors: null
                        },
                        {
                            eventId: '98765432109876543210987654321098765432',
                            notificationRequestId: 'efgh5678',
                            iun: 'EFGH-IJKL-MNOP-123456-N-8',
                            newStatus: "IN_VALIDATION",
                            timestamp: '2024-02-07T14:45:32Z',
                            timelineEventCategory: 'SEND_DIGITAL_DOMICILE',
                            recipientIndex: 2,
                            analogCost: null,
                            channel: 'PEC',
                            legalfactIds: [],
                            validationErrors: null,
                        }
                    ]

                    mock.onGet(url).reply(200, responseBodyV23);

                    const context = {};
                    const response = await consumeEventStreamHandler.handlerEvent(event, context);

                    expect(response.statusCode).to.equal(200);
                    expect(response.body).to.equal(JSON.stringify(responseBodyV10));

                    expect(mock.history.get.length).to.equal(1);
                });

                // L'utilizzo delle properties senza l'element avviene solo nel transitorio
                it("successful request - element = null", async () => {
                    const streamId = "12345";
                    const event = {
                        path: "/delivery-progresses/streams/"+ streamId +"/events",
                        pathParameters : { streamId: streamId },
                        httpMethod: "GET",
                        headers: {},
                        requestContext: {
                            authorizer: {},
                        },
                    }

                    let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events`;

                    const responseBodyV23 = [
                        {
                            eventId: '01234567890123456789012345678901234567',
                            notificationRequestId: 'abcd1234',
                            iun: 'ABCD-EFGH-IJKL-123456-M-7',
                            newStatus: 'ACCEPTED',
                            element: null,
                            timestamp: '2024-02-06T12:34:56Z',
                            timelineEventCategory: 'SENDER_ACK_CREATION_REQUEST',
                            recipientIndex: 1,
                            analogCost: null,
                            channel: 'EMAIL',
                            legalfactIds: ['PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q', 'PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9E'],
                            validationErrors: null
                        },
                        {
                            eventId: '98765432109876543210987654321098765432',
                            notificationRequestId: 'efgh5678',
                            iun: 'EFGH-IJKL-MNOP-123456-N-8',
                            newStatus: 'ACCEPTED',
                            element: null,
                            timestamp: '2024-02-07T14:45:32Z',
                            timelineEventCategory: 'SEND_DIGITAL_DOMICILE',
                            recipientIndex: 2,
                            analogCost: null,
                            channel: 'PEC',
                            legalfactIds: [ 'example_document3.pdf', 'example_document4.pdf' ],
                            validationErrors: null,
                        }
                    ]

                    const responseBodyV10 = [
                        {
                            eventId: '01234567890123456789012345678901234567',
                            notificationRequestId: 'abcd1234',
                            iun: 'ABCD-EFGH-IJKL-123456-M-7',
                            newStatus: 'ACCEPTED',
                            timestamp: '2024-02-06T12:34:56Z',
                            timelineEventCategory: 'SENDER_ACK_CREATION_REQUEST',
                            recipientIndex: 1,
                            analogCost: null,
                            channel: 'EMAIL',
                            legalfactIds: ['PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q', 'PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9E'],
                            validationErrors: null
                        },
                        {
                            eventId: '98765432109876543210987654321098765432',
                            notificationRequestId: 'efgh5678',
                            iun: 'EFGH-IJKL-MNOP-123456-N-8',
                            newStatus: 'ACCEPTED',
                            timestamp: '2024-02-07T14:45:32Z',
                            timelineEventCategory: 'SEND_DIGITAL_DOMICILE',
                            recipientIndex: 2,
                            analogCost: null,
                            channel: 'PEC',
                            legalfactIds: [ 'example_document3.pdf', 'example_document4.pdf' ],
                            validationErrors: null,
                        }
                    ]

                    mock.onGet(url).reply(200, responseBodyV23);

                    const response = await consumeEventStreamHandler.handlerEvent(event, {});

                    expect(response.statusCode).to.equal(200);
                    expect(response.body).to.equal(JSON.stringify(responseBodyV10));

                    expect(mock.history.get.length).to.equal(1);
                });

                it("successful RETURNED_TO_SENDER v25", async () => {

                    const streamId = "12345";
                    const event = {
                        path: "/delivery-progresses/streams/"+ streamId +"/events",
                        pathParameters : { streamId: streamId },
                        httpMethod: "GET",
                        headers: {},
                        requestContext: {
                            authorizer: {},
                        },
                    }

                    let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events`;
                    let urlNotification = `${process.env.BASE_PATH}/delivery-private/notifications/LNWV-GRMV-KPWV-202503-W-1`;
                    let urlStatusHistory = `${process.env.BASE_PATH}/delivery-push-private/LNWV-GRMV-KPWV-202503-W-1/history`;

                    const responseBodyV25 = [
                        {
                            eventId: "01234567890123456789012345678901234567",
                            notificationRequestId: "abcd1234",
                            iun: "LNWV-GRMV-KPWV-202503-W-1",
                            newStatus: "IN_VALIDATION",
                            element: {
                                elementId: "abcdef1234567890",
                                timestamp: "2024-02-06T12:34:56Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [
                                    {
                                        key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                        category: "NOTIFICATION_CANCELLED"
                                    }
                                ],
                                category: "NOTIFICATION_CANCELLED",
                                details: {
                                    recIndex: 1,
                                    digitalAddress: {
                                        type: "EMAIL",
                                        address: "rec@example.com"
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: ""
                                },
                            }
                        },
                        {
                            eventId: "98765432109876543210987654321098765432",
                            notificationRequestId: "efgh5678",
                            iun: "LNWV-GRMV-KPWV-202503-W-1",
                            newStatus: "RETURNED_TO_SENDER",
                            element: {
                                elementId: "ghijkl0987654321",
                                timestamp: "2024-02-07T14:45:32Z",
                                ingestionTimestamp: "2025-02-06T12:34:56Z",
                                eventTimestamp: "2023-02-06T12:34:56Z",
                                notificationSentAt: "2026-02-06T12:34:56Z",
                                legalFactsIds: [
                                    {
                                        key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                        category: "DIGITAL_DELIVERY"
                                    }
                                ],
                                category: "SEND_DIGITAL_DOMICILE",
                                details: {
                                    recIndex: 2,
                                    digitalAddress: {
                                        type: "PEC",
                                        address: "rec@example.com",
                                    },
                                    endWorkflowStatus: {},
                                    completionWorkflowDate: "",
                                },
                            },
                        }
                    ]

                    const responseNotification = {
                        iun: "LNWV-GRMV-KPWV-202503-W-1",
                        recipients: [
                            {
                                denomination: "Mario Cucumber",
                            }
                        ],
                        senderPaId: "e955a1a1-86f0-4d7d-9c2a-f90783f6067c",
                        senderTaxId: "03509990788",
                        sentAt: "2024-03-22T01:14:00.526096073Z",
                    }

                    const responseStatusHistory = {
                        notificationStatus: "VIEWED",
                        notificationStatusHistory: [
                        {
                            status: "ACCEPTED",
                            activeFrom: "2025-04-11T07:47:36.928130418Z"
                        },
                        {
                            status: "DELIVERING",
                            activeFrom: "2024-03-22T01:18:05.244324794Z"
                        },
                        {
                            status: "DELIVERED",
                            activeFrom: "2024-03-22T01:18:20.244324794Z"
                        },
                        {
                            status: "VIEWED",
                            activeFrom: "2024-03-22T01:18:34.706278143Z"
                        },
                        {
                            status: "RETURNED_TO_SENDER",
                            activeFrom: "2024-03-22T01:19:21.622217766Z"
                        }
                        ]
                    }

                    mock.onGet(url).reply(200, responseBodyV25);
                    mock.onGet(urlNotification).reply(200, responseNotification);
                    mock.onGet(urlStatusHistory).reply(200, responseStatusHistory);

                    const response = await consumeEventStreamHandler.handlerEvent(event, {});

                    expect(response.statusCode).to.equal(200);
                    expect(response.body).to.not.contain("RETURNED_TO_SENDER");
                });

        it("Error delivery-private RETURNED_TO_SENDER v25", async () => {

            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams/"+ streamId +"/events",
                pathParameters : { streamId: streamId },
                httpMethod: "GET",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
            }

            let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events`;
            let urlNotification = `${process.env.BASE_PATH}/delivery-private/notifications/LNWV-GRMV-KPWV-202503-W-1`;
            let urlStatusHistory = `${process.env.BASE_PATH}/delivery-push-private/LNWV-GRMV-KPWV-202503-W-1/history`;

            const responseBodyV25 = [
                {
                    eventId: "01234567890123456789012345678901234567",
                    notificationRequestId: "abcd1234",
                    iun: "LNWV-GRMV-KPWV-202503-W-1",
                    newStatus: "IN_VALIDATION",
                    element: {
                        elementId: "abcdef1234567890",
                        timestamp: "2024-02-06T12:34:56Z",
                        ingestionTimestamp: "2025-02-06T12:34:56Z",
                        eventTimestamp: "2023-02-06T12:34:56Z",
                        notificationSentAt: "2026-02-06T12:34:56Z",
                        legalFactsIds: [
                            {
                                key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                category: "NOTIFICATION_CANCELLED"
                            }
                        ],
                        category: "NOTIFICATION_CANCELLED",
                        details: {
                            recIndex: 1,
                            digitalAddress: {
                                type: "EMAIL",
                                address: "rec@example.com"
                            },
                            endWorkflowStatus: {},
                            completionWorkflowDate: ""
                        },
                    }
                },
                {
                    eventId: "98765432109876543210987654321098765432",
                    notificationRequestId: "efgh5678",
                    iun: "LNWV-GRMV-KPWV-202503-W-1",
                    newStatus: "RETURNED_TO_SENDER",
                    element: {
                        elementId: "ghijkl0987654321",
                        timestamp: "2024-02-07T14:45:32Z",
                        ingestionTimestamp: "2025-02-06T12:34:56Z",
                        eventTimestamp: "2023-02-06T12:34:56Z",
                        notificationSentAt: "2026-02-06T12:34:56Z",
                        legalFactsIds: [
                            {
                                key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                category: "DIGITAL_DELIVERY"
                            }
                        ],
                        category: "SEND_DIGITAL_DOMICILE",
                        details: {
                            recIndex: 2,
                            digitalAddress: {
                                type: "PEC",
                                address: "rec@example.com",
                            },
                            endWorkflowStatus: {},
                            completionWorkflowDate: "",
                        },
                    },
                }
            ]

            const responseNotification = {
                    type: "GENERIC_ERROR",
                    status: 404,
                    title: "Handled error",
                    detail: "See logs for details in PN-DELIVERY",
                    traceId: "Root=1-68246234-6c241c9e0ad40c7f27f3593a",
                    timestamp: "2025-05-14T09:28:20.384958167Z",
                    errors: [
                        {
                            code: "PN_DELIVERY_NOTIFICATIONNOTFOUND",
                            detail: "none"
                        }
                    ]
            }

            const responseStatusHistory = {
                notificationStatus: "VIEWED",
                notificationStatusHistory: [
                {
                    status: "ACCEPTED",
                    activeFrom: "2025-04-11T07:47:36.928130418Z"
                },
                {
                    status: "DELIVERING",
                    activeFrom: "2024-03-22T01:18:05.244324794Z"
                },
                {
                    status: "DELIVERED",
                    activeFrom: "2024-03-22T01:18:20.244324794Z"
                },
                {
                    status: "VIEWED",
                    activeFrom: "2024-03-22T01:18:34.706278143Z"
                },
                {
                    status: "RETURNED_TO_SENDER",
                    activeFrom: "2024-03-22T01:19:21.622217766Z"
                }
                ]
            }

            mock.onGet(url).reply(200, responseBodyV25);
            mock.onGet(urlNotification).reply(404, responseNotification);
            mock.onGet(urlStatusHistory).reply(200, responseStatusHistory);

            try {
            const response = await consumeEventStreamHandler.handlerEvent(event, {});

            } catch (e) {
                expect(e.response.status).to.equal(404);
                expect(e.message).to.equal("Call to delivery-private failed with status 404, iun : LNWV-GRMV-KPWV-202503-W-1");
            }
        });

        it("Error delivery-push-private RETURNED_TO_SENDER v25", async () => {

            const streamId = "12345";
            const event = {
                path: "/delivery-progresses/streams/"+ streamId +"/events",
                pathParameters : { streamId: streamId },
                httpMethod: "GET",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
            }

            let url = `${process.env.PN_WEBHOOK_URL}/streams/${streamId}/events`;
            let urlNotification = `${process.env.BASE_PATH}/delivery-private/notifications/LNWV-GRMV-KPWV-202503-W-1`;
            let urlStatusHistory = `${process.env.BASE_PATH}/delivery-push-private/LNWV-GRMV-KPWV-202503-W-1/history`;

            const responseBodyV25 = [
                {
                    eventId: "01234567890123456789012345678901234567",
                    notificationRequestId: "abcd1234",
                    iun: "LNWV-GRMV-KPWV-202503-W-1",
                    newStatus: "IN_VALIDATION",
                    element: {
                        elementId: "abcdef1234567890",
                        timestamp: "2024-02-06T12:34:56Z",
                        ingestionTimestamp: "2025-02-06T12:34:56Z",
                        eventTimestamp: "2023-02-06T12:34:56Z",
                        notificationSentAt: "2026-02-06T12:34:56Z",
                        legalFactsIds: [
                            {
                                key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                category: "NOTIFICATION_CANCELLED"
                            }
                        ],
                        category: "NOTIFICATION_CANCELLED",
                        details: {
                            recIndex: 1,
                            digitalAddress: {
                                type: "EMAIL",
                                address: "rec@example.com"
                            },
                            endWorkflowStatus: {},
                            completionWorkflowDate: ""
                        },
                    }
                },
                {
                    eventId: "98765432109876543210987654321098765432",
                    notificationRequestId: "efgh5678",
                    iun: "LNWV-GRMV-KPWV-202503-W-1",
                    newStatus: "RETURNED_TO_SENDER",
                    element: {
                        elementId: "ghijkl0987654321",
                        timestamp: "2024-02-07T14:45:32Z",
                        ingestionTimestamp: "2025-02-06T12:34:56Z",
                        eventTimestamp: "2023-02-06T12:34:56Z",
                        notificationSentAt: "2026-02-06T12:34:56Z",
                        legalFactsIds: [
                            {
                                key: "safestorage://PN_LEGAL_FACTS-9c3eba7e5fb14c5b9f59635a8edd5714.pdf",
                                category: "DIGITAL_DELIVERY"
                            }
                        ],
                        category: "SEND_DIGITAL_DOMICILE",
                        details: {
                            recIndex: 2,
                            digitalAddress: {
                                type: "PEC",
                                address: "rec@example.com",
                            },
                            endWorkflowStatus: {},
                            completionWorkflowDate: "",
                        },
                    },
                }
            ]

            const responseNotification = {
                iun: "LNWV-GRMV-KPWV-202503-W-1",
                recipients: [
                    {
                        denomination: "Mario Cucumber",
                    }
                ],
                senderPaId: "e955a1a1-86f0-4d7d-9c2a-f90783f6067c",
                senderTaxId: "03509990788",
                sentAt: "2024-03-22T01:14:00.526096073Z",
            }

            const responseStatusHistory = {
                notificationStatus: "IN_VALIDATION",
                notificationStatusHistory: [],
                timeline: []
            }

            mock.onGet(url).reply(200, responseBodyV25);
            mock.onGet(urlNotification).reply(200, responseNotification);
            mock.onGet(urlStatusHistory).reply(200, responseStatusHistory);

            try {
            const response = await consumeEventStreamHandler.handlerEvent(event, {});

            } catch (e) {
                expect(e.message).to.equal('Call to delivery-private failed, [iun : LNWV-GRMV-KPWV-202503-W-1, createdAt : 2024-03-22T01:14:00.526096073Z, numberOfRecipients : 1 ]');
            }
        });
    });

});