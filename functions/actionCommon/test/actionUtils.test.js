const {getQueueNameFromParameterStore, getQueueName} = require("../app/actionUtils");
const axios = require('axios');
const chaiAsPromised = require("chai-as-promised");
const chai = require("chai");

var MockAdapter = require("axios-mock-adapter");
chai.use(chaiAsPromised);
const expect = chai.expect;

describe("action utils test", function() {
    let mock;

    before(() => {
        mock = new MockAdapter(axios);
        process.env = Object.assign(process.env, {
            ACTION_QUEUE_MAP : "[{\"tipologiaAzione\":\"DOCUMENT_CREATION_RESPONSE_SENDER_ACK\",\"queueName\":\"delivery_push_queue_sender_ack\"},{\"tipologiaAzione\":\"DOCUMENT_CREATION_RESPONSE\",\"queueName\":\"delivery_push_queue_no_sender_ack\"},{\"tipologiaAzione\":\"NOTIFICATION_CREATION\",\"queueName\":\"delivery_push_queue_creation\"},{\"tipologiaAzione\":\"NOTIFICATION_VALIDATION\",\"queueName\":\"delivery_push_queue_validation\"}]"
          });
    });

    afterEach(() => {
        mock.reset();
    });

    after(() => {
        mock.restore();
    });

    it("get queue name no details - success", async () => {
        const actionType = "NOTIFICATION_VALIDATION";
        const expectedQueueName = "delivery_push_queue_validation";
        
        const queueName = await getQueueName(actionType, null);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name with details - success", async () => {
        const actionType = "DOCUMENT_CREATION_RESPONSE";
        const details = { documentCreationType: 'NO_SENDER_ACK' }
        const expectedQueueName = "delivery_push_queue_no_sender_ack";
        
        const queueName = await getQueueName(actionType, details);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name with details SENDER_ACK - success", async () => {
        const actionType = "DOCUMENT_CREATION_RESPONSE";
        const details = { documentCreationType: 'SENDER_ACK' }
        const expectedQueueName = "delivery_push_queue_sender_ack";
        
        const queueName = await getQueueName(actionType, details);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name no details - fail", async () => {
        const actionType = "VALIDATION";

        await expect(
            getQueueName(actionType, null)
          ).to.be.rejectedWith(Error, "Unable to find queue");
    });

    it("get queue name from PS no details - success", async () => {
        const actionType = "NOTIFICATION_VALIDATION";
        const expectedQueueName = "delivery_push_queue_validation";
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "[{\"tipologiaAzione\":\"NOTIFICATION_VALIDATION\",\"queueName\":\"delivery_push_queue_validation\"},{\"tipologiaAzione\":\"NOTIFICATION_CREATION\",\"queueName\":\"delivery_push_queue_creation\"}]";
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
        const parameterValue = "[{\"tipologiaAzione\":\"DOCUMENT_CREATION_RESPONSE\",\"queueName\":\"delivery_push_queue_validation\"},{\"tipologiaAzione\":\"NOTIFICATION_CREATION\",\"queueName\":\"delivery_push_queue_creation\"}]";
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
        const parameterValue = "[{\"tipologiaAzione\":\"DOCUMENT_CREATION_RESPONSE_SENDER_ACK\",\"queueName\":\"delivery_push_queue_validation\"},{\"tipologiaAzione\":\"NOTIFICATION_CREATION\",\"queueName\":\"delivery_push_queue_creation\"}]";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})
        
        const queueName = await getQueueNameFromParameterStore(actionType, details, parameterStoreName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name from PS no details - fail", async () => {
        const actionType = "VALIDATION";
        const expectedQueueName = "delivery_push_queue_validation";
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "[{\"tipologiaAzione\":\"NOTIFICATION_VALIDATION\",\"queueName\":\"delivery_push_queue_validation\"},{\"tipologiaAzione\":\"NOTIFICATION_CREATION\",\"queueName\":\"delivery_push_queue_creation\"}]";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})

        await expect(
            getQueueNameFromParameterStore(actionType, null, parameterStoreName)
          ).to.be.rejectedWith(Error, "Unable to find queue");
    });

});