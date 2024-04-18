const axios = require("axios");
const EventHandler  = require('./baseHandler.js');
const { createStreamMetadataResponseV10 } = require("./mapper/transformStreamMetadataResponseFromV23ToV10.js");
const { createStreamCreationRequestV22 } = require("./mapper/transformStreamCreationRequestFromV10ToV23.js");

class CreateEventStreamHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context) {
        const {path, httpMethod} = event;
        return (path.endsWith('/streams') || path.endsWith('/streams/')) && httpMethod.toUpperCase() === 'POST';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.x_CreateEventStream_Lambda function started");

        // HEADERS
        const headers = this.prepareHeaders(event);

        // REQUEST BODY
        const requestBodyV1 = JSON.parse(event.body);
        const requestBodyV22 = createStreamCreationRequestV22(requestBodyV1);
        const url = `${this.baseUrl}/streams`;

        console.log('calling ', url);
        console.log(requestBodyV22);
        let response = await axios.post(url, requestBodyV22, {headers: headers});
        // RESPONSE BODY
        const transformedObject = createStreamMetadataResponseV10(response.data);

        return {
            statusCode: response.status,
            body: JSON.stringify(transformedObject),
        };
    }
}

module.exports = CreateEventStreamHandler;