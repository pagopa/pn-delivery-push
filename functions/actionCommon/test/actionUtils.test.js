const {getQueueName} = require("../app/actionUtils");
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
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "[{\"tipologiaAzione\":\"NOTIFICATION_VALIDATION\",\"queueName\":\"delivery_push_queue_validation\"},{\"tipologiaAzione\":\"NOTIFICATION_CREATION\",\"queueName\":\"delivery_push_queue_creation\"}]";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})
        
        const queueName = await getQueueName(actionType, null, parameterStoreName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name with details - success", async () => {
        const actionType = "DOCUMENT_CREATION_RESPONSE";
        const details = { documentCreationType: 'NO_SENDER_ACK' }
        const expectedQueueName = "delivery_push_queue_validation";
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "[{\"tipologiaAzione\":\"DOCUMENT_CREATION_RESPONSE\",\"queueName\":\"delivery_push_queue_validation\"},{\"tipologiaAzione\":\"NOTIFICATION_CREATION\",\"queueName\":\"delivery_push_queue_creation\"}]";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})
        
        const queueName = await getQueueName(actionType, details, parameterStoreName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name with details SENDER_ACK - success", async () => {
        const actionType = "DOCUMENT_CREATION_RESPONSE";
        const details = { documentCreationType: 'SENDER_ACK' }
        const expectedQueueName = "delivery_push_queue_validation";
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "[{\"tipologiaAzione\":\"DOCUMENT_CREATION_RESPONSE_SENDER_ACK\",\"queueName\":\"delivery_push_queue_validation\"},{\"tipologiaAzione\":\"NOTIFICATION_CREATION\",\"queueName\":\"delivery_push_queue_creation\"}]";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})
        
        const queueName = await getQueueName(actionType, details, parameterStoreName);
        expect(queueName).to.equal(expectedQueueName);
    });

    it("get queue name no details - fail", async () => {
        const actionType = "VALIDATION";
        const expectedQueueName = "delivery_push_queue_validation";
        const parameterStoreName = "parameterStoreName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterStoreName)}`;
        const parameterValue = "[{\"tipologiaAzione\":\"NOTIFICATION_VALIDATION\",\"queueName\":\"delivery_push_queue_validation\"},{\"tipologiaAzione\":\"NOTIFICATION_CREATION\",\"queueName\":\"delivery_push_queue_creation\"}]";
        mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: parameterValue } } ), {"Content-Type": "application/json"})

        await expect(
            getQueueName(actionType, null, parameterStoreName)
          ).to.be.rejectedWith(Error, "Unable to find queue");
    });

});