const axios = require("axios");
const axiosRetry = require("axios-retry").default;
const EventHandler  = require('./baseHandler.js');
const {createProgressResponseV10} = require("./mapper/transformProgressResponseFromV23ToV10");
class ConsumeEventStreamHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
        return path.includes('/streams') && (path.endsWith('/events') || path.endsWith('/events/')) && event["pathParameters"] && httpMethod.toUpperCase() === 'GET';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.x_ConsumeEventStream_Lambda function started");
        // HEADERS
        const headers = this.prepareHeaders(event);

        const streamId = event["pathParameters"]["streamId"];
        const lastEventId = event["queryStringParameters"]==null?null:event["queryStringParameters"]["lastEventId"];
        let lastEventIdQueryParam = "";
        if (lastEventId != null && lastEventId !== undefined && lastEventId !== "")
          lastEventIdQueryParam = `?lastEventId=${lastEventId}`;
        let url = `${this.baseUrl}/streams/${streamId}/events${lastEventIdQueryParam}`;

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
        
        response = await axios.get(url, {headers: headers, timeout: this.attemptTimeout});

        // RESPONSE BODY
        // Il controllo della presenza di element avviene solo nel transitorio
        let responseBody = [];
        for(const data of response.data) {
            if (data.element)
                responseBody.push(createProgressResponseV10(data));
            else{
                delete data.element;
                responseBody.push(data);
            }
        }

        return {
            statusCode: response.status,
            headers: response.headers,
            body: JSON.stringify(responseBody)
        };
    }
}

module.exports = ConsumeEventStreamHandler;