const axios = require("axios");
const axiosRetry = require("axios-retry").default;
const EventHandler  = require('./baseHandler.js');
const { createStreamMetadataResponseV10 } = require("./mapper/transformStreamMetadataResponseFromV23ToV10.js");
const { createStreamMetadataResponseV26 } = require("./mapper/transformStreamMetadataResponseFromV27ToV26");


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
        let version = this.getVersion(event);
        const headers = this.prepareHeaders(event, version);

        const streamId = event["pathParameters"]["streamId"];
        const url = `${this.baseUrl}/streams/${streamId}`;
        axiosRetry(axios, {
            retries: this.numRetry,
            shouldResetTimeout: true ,
            retryCondition: (error) => {
              return axiosRetry.isNetworkOrIdempotentRequestError(error) || error.code === 'ECONNABORTED';
            },
            onRetry: (retryCount, error, requestConfig) => {
                console.warn(`Retry num ${retryCount} - error:${error.message}`);
            },
            onMaxRetryTimesExceeded: (error, retryCount) => {
                console.warn(`Retries exceeded: ${retryCount} - error:${error.message}`);
            }
        });

        console.log('calling ', url);
        let response = await axios.get(url, {headers: headers, timeout: this.attemptTimeout});

        // RESPONSE BODY
        let transformedObject;

        switch(version) {
            case 10:
                transformedObject = createStreamMetadataResponseV10(createStreamMetadataResponseV26(response.data));
            break;
            case 23:
            case 24:
            case 25:
            case 26:
                transformedObject = createStreamMetadataResponseV26(response.data);
            break;
            default:
                console.error('Invalid version ', version)
            break;
        }
        
        const ret = {
            statusCode: response.status,
            body: JSON.stringify(transformedObject),
        }
        return ret;
    }
}

module.exports = GetEventStreamHandler;