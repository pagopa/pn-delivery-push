const axios = require("axios");
const axiosRetry = require("axios-retry").default;
const EventHandler  = require('./baseHandler.js');
const { createStreamMetadataResponseV10 } = require("./mapper/transformStreamMetadataResponseFromV23ToV10.js");
const { createStreamRequestV22 } = require("./mapper/transformStreamRequestFromV10ToV23")
class UpdateEventStreamHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
        return path.includes('/streams') && event["pathParameters"] !== undefined && event["pathParameters"] != null && httpMethod.toUpperCase() === 'PUT';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.x_UpdateEventStream_Lambda function started");

        // HEADERS
        let version = this.getVersion(event);
        const headers = this.prepareHeaders(event, version);

        // REQUEST BODY
        const requestBodyV1 = JSON.parse(event.body);
        const requestBodyV22= createStreamRequestV22(requestBodyV1);

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
        console.log(requestBodyV22);
        let response = await axios.put(url, requestBodyV22, {headers: headers, timeout: this.attemptTimeout});

        // RESPONSE BODY
        let transformedObject;
        
        switch(version) {
            case 10:
                transformedObject = createStreamMetadataResponseV10(response.data);
            break;
            case 23:
                transformedObject = response.data;
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

module.exports = UpdateEventStreamHandler;