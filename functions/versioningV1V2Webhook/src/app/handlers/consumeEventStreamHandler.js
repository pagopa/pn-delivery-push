const axios = require("axios");
const EventHandler  = require('./baseHandler.js');
const {createProgressResponseV10} = require("./mapper/transformProgressResponseFromV23ToV10");
class ConsumeEventStreamHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
        return path.includes('/streams') && path.endsWith('/events') && event["pathParameters"] && httpMethod.toUpperCase() === 'GET';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.3_ConsumeEventStream_Lambda function started");
        // HEADERS
        const headers = this.setHeaders(event);

        const streamId = event["pathParameters"]["streamId"];
        const url = `${this.baseUrl}/streams/${streamId}/events`;

        console.log('calling ', url);
        let response = await axios.get(url, {headers: headers});
        // RESPONSE BODY
        // Il controllo della presenza di element avviene solo nel transitorio
        let responseBody;
        if(response.data.element)
            responseBody = createProgressResponseV10(response.data);
        else
            responseBody = response.data;

        const ret = {
            statusCode: response.status,
            headers: response.headers,
            body: JSON.stringify(responseBody)
        }
        return ret;
    }
}

module.exports = ConsumeEventStreamHandler;