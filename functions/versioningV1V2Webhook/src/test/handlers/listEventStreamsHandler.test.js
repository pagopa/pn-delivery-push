const { expect } = require("chai");
const axios = require("axios");
const ListEventStreamsHandler= require("../../app/handlers/listEventStreamsHandler.js");

let MockAdapter = require("axios-mock-adapter")
let mock = new MockAdapter(axios);

describe("ListEventStreamsHandler", () => {

    let listEventStreamsHandler;

    beforeEach(() => {
        listEventStreamsHandler = new ListEventStreamsHandler();
        mock = new MockAdapter(axios);
    });

    afterEach(() => {
        mock.restore();
    });

    describe("checkOwnership", () => {
        it("valid ownership - case 1", () => {
            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "GET" };
            const result = listEventStreamsHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("valid ownership - case 2", () => {
            const event = {
                path: "/delivery-progresses/streams/",
                httpMethod: "GET" };
            const result = listEventStreamsHandler.checkOwnership(event, {});
            expect(result).to.be.true;
        });

        it("invalid ownership - case 1", () => {
            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "POST" };
            const result = listEventStreamsHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });

        it("invalid ownership - case 2", () => {
            const event = {
                path: "delivery-progresses/streams/{streamId}",
                httpMethod: "GET" };
            const result = listEventStreamsHandler.checkOwnership(event, {});
            expect(result).to.be.false;
        });
    });

    describe("handlerEvent", () => {

        process.env = Object.assign(process.env, {
            PN_WEBHOOK_URL: "https://api.dev.notifichedigitali.it/delivery-progresses/v2.3",
        });

        it("successful request", async () => {
            const event = {
                path: "/delivery-progresses/streams",
                httpMethod: "GET",
                headers: {},
                requestContext: {
                    authorizer: {},
                },
            };

            let url = `${process.env.PN_WEBHOOK_URL}/streams`;

            const responseBody = [
                {
                title: "stream name 1",
                streamId: "12345678-90ab-cdef-ghij-klmnopqrstuv"
                },
                {
                    title: "stream name 2",
                    streamId: "abcdefgh-ijkl-mnop-qrst-uvwxyz123456"
                }
            ]

            mock.onGet(url).reply(200, responseBody);

            const context = {};
            const response = await listEventStreamsHandler.handlerEvent(event, context);

            expect(response.statusCode).to.equal(200);
            expect(response.body).to.equal(JSON.stringify(responseBody));

            expect(mock.history.get.length).to.equal(1);
        });
    });
});