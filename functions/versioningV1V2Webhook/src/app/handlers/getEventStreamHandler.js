const axios = require("axios");
const EventHandler  = require('./baseHandler.js');
const { createStreamMetadataResponseV10 } = require("./mapper/transformStreamMetadataResponseFromV23ToV10.js");

class GetEventStreamHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
        return path.includes('/streams') && event["pathParameters"] !== undefined && event["pathParameters"] != null && httpMethod.toUpperCase() === 'GET';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.x_GetEventStream_Lambda function started");

        // HEADERS
        const headers = this.prepareHeaders(event);

        const streamId = event["pathParameters"]["streamId"];
        const url = `${this.baseUrl}/streams/${streamId}`;

        console.log('calling ', url);
        let response;
        let lastError = null;
        for (var i=0; i< this.numRetry; i++) {
            console.log('attempt #',i);
            try {
                response = await axios.get(url, {headers: headers, timeout: this.attemptTimeout});
                if (response) {
                    lastError = null;
                    break;
                } else {
                  console.log('cannot fetch data');
                }
            } catch (error) {
                lastError = error;
                console.log('cannot fetch data');
            }
        }

        if (lastError != null) {
            throw lastError;
        }

        // RESPONSE BODY
        const transformedObject = createStreamMetadataResponseV10(response.data);

        const ret = {
            statusCode: response.status,
            body: JSON.stringify(transformedObject),
        }
        return ret;
    }
}

module.exports = GetEventStreamHandler;