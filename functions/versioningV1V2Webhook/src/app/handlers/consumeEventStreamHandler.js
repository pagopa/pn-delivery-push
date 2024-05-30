const axios = require("axios");
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