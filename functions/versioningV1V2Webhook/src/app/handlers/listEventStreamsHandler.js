const axios = require("axios");
const axiosRetry = require("axios-retry").default;

const EventHandler  = require('./baseHandler.js');
class ListEventStreamsHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
        return (path.endsWith('/streams') || path.endsWith('/streams/')) && httpMethod.toUpperCase() === 'GET';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.x_ListEventStreams_Lambda function started");

        // HEADERS
        const headers = this.prepareHeaders(event);
        const url = `${this.baseUrl}/streams`;
        axiosRetry(axios, {
            retries: numRetry,
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
        response= await axios.get(url, {headers: headers, timeout: this.attemptTimeout});

        return {
            statusCode: response.status,
            body: JSON.stringify(response.data),
        };
    }
}

module.exports = ListEventStreamsHandler;