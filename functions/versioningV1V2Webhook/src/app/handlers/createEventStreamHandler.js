const axios = require("axios");
const EventHandler  = require('./baseHandler.js');
const { createStreamMetadataResponseV10 } = require("./mapper/transformStreamMetadataResponseFromV23ToV10.js");
const { createStreamCreationRequestV22 } = require("./mapper/transformStreamCreationRequestFromV10ToV23.js");
const { createStreamMetadataResponseV26 } = require("./mapper/transformStreamMetadataResponseFromV27ToV26");
const { createStreamCreationRequestV26 } = require("./mapper/transformStreamCreationRequestFromV27ToV26");

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
        let version = this.getVersion(event);
        const headers = this.prepareHeaders(event, version);

        // REQUEST BODY
        let requestBody = JSON.parse(event.body);
        switch(version) {
            case 10:
                requestBody = createStreamCreationRequestV22(requestBody);
            break;
            case 23:
            case 24:
            case 25:
            case 26:
                requestBody = createStreamCreationRequestV26(requestBody);
            break;
            default:
                console.error('Invalid version ', version)
            break;
        }
        
        const url = `${this.baseUrl}/streams`;

        console.log('calling ', url);
        console.log(requestBody);

        let postTimeout = this.attemptTimeout * this.numRetry;
        let response = await axios.post(url, requestBody, {headers: headers, timeout: postTimeout});

        let transformedObject;

        // RESPONSE BODY
        switch(version) {
            case 10:
                transformedObject = createStreamMetadataResponseV10(response.data);
            break;
            case 23:
                transformedObject = createStreamMetadataResponseV26(response.data);
            break;
            case 24:
                transformedObject = createStreamMetadataResponseV26(response.data);
            break;
            case 25:
                transformedObject = createStreamMetadataResponseV26(response.data);
            break;
            case 26:
                transformedObject = createStreamMetadataResponseV26(response.data);
            break;
            default:
                console.error('Invalid version ', version)
            break;
        }

        return {
            statusCode: response.status,
            body: JSON.stringify(transformedObject),
        };
    }
}

module.exports = CreateEventStreamHandler;