const { insertAction } = require("../app/lib/client.js");
const axios = require("axios");
const chai = require("chai");
const sinon = require("sinon");
const MockAdapter = require("axios-mock-adapter");

const expect = chai.expect;

describe("insertAction", function () {
    let mock;
    let originalEnv;
    let logStub, warnStub, errorStub;

    before(() => {
        mock = new MockAdapter(axios);
    });

    beforeEach(() => {
        originalEnv = { ...process.env };
        process.env.ACTION_MANAGER_BASE_URL = "http://localhost:3000";
        logStub = sinon.stub(console, "log");
        warnStub = sinon.stub(console, "warn");
        errorStub = sinon.stub(console, "error");
    });

    afterEach(() => {
        process.env = originalEnv;
        mock.reset();
        logStub.restore();
        warnStub.restore();
        errorStub.restore();
    });

    after(() => {
        mock.restore();
    });

    it("throws if ACTION_MANAGER_BASE_URL is not set", async () => {
        delete process.env.ACTION_MANAGER_BASE_URL;
        try {
            await insertAction([{ actionId: "a1" }]);
            // If no error is thrown, fail the test
            throw new Error("Expected error was not thrown");
        } catch (err) {
            expect(err.message).to.equal("ACTION_MANAGER_BASE_URL env is not set");
        }
    });

    it("inserts one event successfully", async () => {
        const event = { actionId: "a1", iun: "iun1", type: "TYPE", notBefore: 123, timelineId: "t1" };
        mock
            .onPost("http://localhost:3000/action-manager-private/action")
            .reply(201, {});
        const result = await insertAction([event]);
        expect(result).to.deep.equal({ insertions: 1, errors: [] });
        expect(logStub.calledWithMatch(/Action inserted successfully/)).to.be.true;
    });

    it("handles 409 conflict and skips error", async () => {
        const event = { actionId: "a2", iun: "iun2", type: "TYPE", notBefore: 123, timelineId: "t2" };
        mock
            .onPost("http://localhost:3000/action-manager-private/action")
            .reply(409, {});
        const error = new Error("Conflict");
        error.response = { status: 409 };
        mock.onPost().reply(() => { throw error; });
        const result = await insertAction([event]);
        expect(result).to.deep.equal({ insertions: 0, errors: [] });
        expect(warnStub.calledWithMatch(/Action already exists/)).to.be.true;
    });

    it("handles other errors and adds to errors array", async () => {
        const event = { actionId: "a3", iun: "iun3", type: "TYPE", notBefore: 123, timelineId: "t3" };
        const error = new Error("Server error");
        error.response = { status: 500 };
        mock.onPost().reply(() => { throw error; });
        const result = await insertAction([event]);
        expect(result).to.deep.equal({ insertions: 0, errors: [event] });
        expect(errorStub.calledWithMatch(/Error putting action/)).to.be.true;
    });

    it("handles multiple events with mixed results", async () => {
        const events = [
            { actionId: "a1", iun: "iun1", type: "TYPE", notBefore: 1, timelineId: "t1" }, // 201
            { actionId: "a2", iun: "iun2", type: "TYPE", notBefore: 2, timelineId: "t2" }, // 409
            { actionId: "a3", iun: "iun3", type: "TYPE", notBefore: 3, timelineId: "t3" }, // 500
        ];

        mock.onPost("http://localhost:3000/action-manager-private/action").replyOnce(201, {});
        mock.onPost("http://localhost:3000/action-manager-private/action").replyOnce(() => {
            const error = new Error("Conflict");
            error.response = { status: 409 };
            throw error;
        });
        mock.onPost("http://localhost:3000/action-manager-private/action").replyOnce(() => {
            const error = new Error("Server error");
            error.response = { status: 500 };
            throw error;
        });
        const result = await insertAction(events);
        expect(result).to.deep.equal({ insertions: 1, errors: [events[2]] });
        expect(logStub.calledWithMatch(/Action inserted successfully/)).to.be.true;
        expect(warnStub.calledWithMatch(/Action already exists/)).to.be.true;
        expect(errorStub.calledWithMatch(/Error putting action/)).to.be.true;
    });
});