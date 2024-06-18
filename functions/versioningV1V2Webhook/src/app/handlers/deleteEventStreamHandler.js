const axios = require("axios");
const axiosRetry = require("axios-retry").default;
const EventHandler  = require('./baseHandler.js');
class DeleteEventStreamHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
        return path.includes('/streams') && event["pathParameters"] !== undefined && event["pathParameters"] != null && httpMethod.toUpperCase() === 'DELETE';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.x_DeleteEventStream_Lambda function started");

        // HEADERS
        const headers = this.prepareHeaders(event);
        headers["Accept"] = "*/*";

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
        let response;
        response = await axios.delete(url, { headers: headers, timeout: this.attemptTimeout});

        const ret = {
            statusCode: response.status,
            body: response.data
        }
        return ret;
    }
}

module.exports = DeleteEventStreamHandler;