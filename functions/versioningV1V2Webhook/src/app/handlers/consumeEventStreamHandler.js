const axios = require("axios");
const EventHandler  = require('./baseHandler.js');
const {createProgressResponseV10} = require("./mapper/transformProgressResponseFromV23ToV10");
class ConsumeEventStreamHandler extends EventHandler {
    constructor() {
        super();
    }

    checkOwnership(event, context){
        const {path, httpMethod} = event;
        return path.includes('/streams') && path.endsWith('/events') && event["pathParameters"] && httpMethod.toUpperCase() === 'GET';
    }

    async handlerEvent(event, context) {
        console.log("Versioning_V1-V2.x_ConsumeEventStream_Lambda function started");
        // HEADERS
        const headers = this.prepareHeaders(event);

        const streamId = event["pathParameters"]["streamId"];
        const lastEventId = event["queryStringParameters"]==null?null:event["queryStringParameters"]["lastEventId"];
        let lastEventIdQueryParam = "";
        if (lastEventId != null && lastEventId !== undefined && lastEventId != "")
          lastEventIdQueryParam = `?lastEventId=${lastEventId}`;
        let url = `${this.baseUrl}/streams/${streamId}/events${lastEventIdQueryParam}`;



        console.log('calling ', url);
        let response = await axios.get(url, {headers: headers});

        // RESPONSE BODY
        // Il controllo della presenza di element avviene solo nel transitorio
        let responseBody = [];
        for(let i=0; i < response.data.length; i++){
            if(response.data[i].element)
                responseBody.push(createProgressResponseV10(response.data[i]));
            else{
                delete response.data[i].element;
                responseBody.push(response.data[i]);
            }
        }

        const ret = {
            statusCode: response.status,
            headers: response.headers,
            body: JSON.stringify(responseBody)
        }
        return ret;
    }
}

module.exports = ConsumeEventStreamHandler;