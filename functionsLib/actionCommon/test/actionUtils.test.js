const {getQueueNameFromParameterStore, getQueueName} = require("../app/actionUtils");
const axios = require('axios');
const chaiAsPromised = require("chai-as-promised");
const chai = require("chai");

var MockAdapter = require("axios-mock-adapter");
chai.use(chaiAsPromised);
const expect = chai.expect;

describe("action utils test", function() {
    let mock;
    let envVarName = "ACTION_QUEUE_MAP";

    before(() => {
        mock = new MockAdapter(axios);
        process.env = Object.assign(process.env, {
            ACTION_QUEUE_MAP : "{\"DOCUMENT_CREATION_RESPONSE_SENDER_ACK\":\"delivery_push_queue_sender_ack\",\"DOCUMENT_CREATION_RESPONSE\"\:\"delivery_push_queue_no_sender_ack\",\"NOTIFICATION_CREATION\":\"delivery_push_queue_creation\",\"NOTIFICATION_VALIDATION\":\"delivery_push_queue_validation\"}"
          });
    });

    afterEach(() => {
        mock.reset();
    });

    after(() => {
        mock.restore();
    });

    it("get queue name wrong env var name - fail", async () => {
        const actionType = "NOTIFICATION_VALIDATION";

        await expect(
            getQueueName(actionType, null, "WRONG_ACTION_QUEUE_MAP")
          ).to.be.rejectedWith(Error, "Invalid env var value");
    });

    it("get queue name no details - success", async () => {
        const actionType = "NOTIFICATION_VALIDATION";
        const expectedQueueName = "delivery_push_queue_validation";
        
        const queueName = await getQueueName(actionType, null, envVarName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name with details - success", async () => {
        const actionType = "DOCUMENT_CREATION_RESPONSE";
        const details = { documentCreationType: 'NO_SENDER_ACK' }
        const expectedQueueName = "delivery_push_queue_no_sender_ack";
        
        const queueName = await getQueueName(actionType, details, envVarName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name with details SENDER_ACK - success", async () => {
        const actionType = "DOCUMENT_CREATION_RESPONSE";
        const details = { documentCreationType: 'SENDER_ACK' }
        const expectedQueueName = "delivery_push_queue_sender_ack";
        
        const queueName = await getQueueName(actionType, details, envVarName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name no details - fail", async () => {
        const actionType = "VALIDATION";

        await expect(
            getQueueName(actionType, null, envVarName)
          ).to.be.rejectedWith(Error, "Unable to find queue");
    });

    it("get queue name from PS no details - success", async () => {
        const actionType = "NOTIFICATION_VALIDATION";
        const expectedQueueName = "delivery_push_queue_validation";
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "{\"NOTIFICATION_VALIDATION\":\"delivery_push_queue_validation\",\"NOTIFICATION_CREATION\":\"delivery_push_queue_creation\"}";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})
        
        const queueName = await getQueueNameFromParameterStore(actionType, null, parameterStoreName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name from PS with details - success", async () => {
        const actionType = "DOCUMENT_CREATION_RESPONSE";
        const details = { documentCreationType: 'NO_SENDER_ACK' }
        const expectedQueueName = "delivery_push_queue_validation";
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "{\"DOCUMENT_CREATION_RESPONSE\":\"delivery_push_queue_validation\",\"NOTIFICATION_CREATION\":\"delivery_push_queue_creation\"}";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})
        
        const queueName = await getQueueNameFromParameterStore(actionType, details, parameterStoreName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name from PS with details SENDER_ACK - success", async () => {
        const actionType = "DOCUMENT_CREATION_RESPONSE";
        const details = { documentCreationType: 'SENDER_ACK' }
        const expectedQueueName = "delivery_push_queue_validation";
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "{\"DOCUMENT_CREATION_RESPONSE_SENDER_ACK\":\"delivery_push_queue_validation\",\"NOTIFICATION_CREATION\":\"delivery_push_queue_creation\"}";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})
        
        const queueName = await getQueueNameFromParameterStore(actionType, details, parameterStoreName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name from PS no details - fail", async () => {
        const actionType = "VALIDATION";
        const expectedQueueName = "delivery_push_queue_validation";
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "{\"NOTIFICATION_VALIDATION\":\"delivery_push_queue_validation\",\"NOTIFICATION_CREATION\":\"delivery_push_queue_creation\"}";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})

        await expect(
            getQueueNameFromParameterStore(actionType, null, parameterStoreName)
          ).to.be.rejectedWith(Error, "Unable to find queue");
    });

});