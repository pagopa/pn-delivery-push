const axios = require("axios");
const axiosRetry = require("axios-retry").default;
const EventHandler  = require('./baseHandler.js');
const {createProgressResponseV10} = require("./mapper/transformProgressResponseFromV23ToV10");
const {createProgressResponseV23} = require("./mapper/transformProgressResponseFromV24ToV23");

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

        // ora è necessario sapere da che versione sto invocando, per prendere le decisioni corrette.
        let version = 10;
        
        if (event["path"].includes("v2.3")) {
            version = 23;
        }

        // NB: sebbene (a oggi) la 2.4 non passa di qua, in futuro potrebbe e quindi si è già implementata
        // la logica di traduzione (che probabilmente andrà aggiornata nel futuro)
        if (event["path"].includes("v2.4")) {
            version = 24;
        }

        console.log('version is ', version);

        // RESPONSE BODY
        // Il controllo della presenza di element avviene solo nel transitorio
        let responseBody = [];
        for(const data of response.data) {
            if (data.element){
                switch(version) {
                    case 10:
                        console.debug('Mapping to v10')
                        responseBody.push(createProgressResponseV10(createProgressResponseV23(data)));
                    break;
                    case 23:
                        console.debug('Mapping to v23')
                        responseBody.push(createProgressResponseV23(data));
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