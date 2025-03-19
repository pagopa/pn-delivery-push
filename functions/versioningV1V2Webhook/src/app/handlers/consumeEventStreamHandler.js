const axios = require("axios");
const axiosRetry = require("axios-retry").default;
const EventHandler  = require('./baseHandler.js');
const {createProgressResponseV10} = require("./mapper/transformProgressResponseFromV23ToV10");
const {createProgressResponseV23} = require("./mapper/transformProgressResponseFromV24ToV23");
const {createProgressResponseV24} = require("./mapper/transformProgressResponseFromV25ToV24");

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
        let version = this.getVersion(event);
        const headers = this.prepareHeaders(event, version);

        const streamId = event["pathParameters"]["streamId"];
        const lastEventId = event["queryStringParameters"]==null?null:event["queryStringParameters"]["lastEventId"];
        let lastEventIdQueryParam = "";
        if (lastEventId != null && lastEventId !== undefined && lastEventId !== "")
          lastEventIdQueryParam = `?lastEventId=${lastEventId}`;
        let url = `${this.baseUrl}/streams/${streamId}/events${lastEventIdQueryParam}`;

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
        // Il controllo della presenza di element avviene solo nel transitorio
        let responseBody = [];
        for(const data of response.data) {
            if (data.element){
                switch(version) {
                    case 10:
                        console.debug('Mapping to v10')
                        responseBody.push(createProgressResponseV10(createProgressResponseV23(createProgressResponseV24(data))));
                    break;
                    case 23:
                        console.debug('Mapping to v23')
                        responseBody.push(createProgressResponseV23(createProgressResponseV24(data)));
                    break;
                    case 24:
                        console.debug('Mapping to v24')
                        responseBody.push(createProgressResponseV24(data));
                    break;
                    case 25:
                        console.debug('Mapping to v25')
                        responseBody.push(data);
                    break;
                    case 26:
                        console.debug('Mapping to v26')
                        responseBody.push(data);
                    break;
                    case 27:
                        console.debug('Mapping to v27')
                        responseBody.push(data);
                    break;
                    default:
                        console.error('Invalid version ', version)
                    break;
                  }
            }else{
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