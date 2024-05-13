const {getParameterFromLayer} = require("../app/utils");
const axios = require('axios');
const chaiAsPromised = require("chai-as-promised");
const chai = require("chai");

var MockAdapter = require("axios-mock-adapter");
chai.use(chaiAsPromised);
const expect = chai.expect;

describe("utils test", function() {
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

    it("should get parameter from layer - success", async () => {
    const parameterName = "parameterName";
    const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterName)}`;
    mock.onGet(url).reply(200, JSON.stringify( { Parameter: { Value: "parameterValue" } } ), {"Content-Type": "application/json"})
    const parameter = await getParameterFromLayer(parameterName);
    console.log("parameterValue: ", parameter);
    expect(parameter).to.equal("parameterValue");
    });

    it("should get parameter from layer - fail", async () => {
        const parameterName = "parameterName";
        const url = `http://localhost:2773/systemsmanager/parameters/get?name=${encodeURIComponent(parameterName)}`;
        mock.onGet(url).reply(500);
        await expect(
            getParameterFromLayer(parameterName)
        ).to.be.rejectedWith(Error, "Unable to get parameter");
    }).timeout(4000);
})

