const axios = require("axios");
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

        console.log('calling ', url);
        let response;
        let lastError = null;
        for (var i=0; i< this.numRetry; i++) {
            console.log('attempt #',i);
            try{
                response= await axios.get(url, {headers: headers, timeout: this.attemptTimeout});
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

        return {
            statusCode: response.status,
            body: JSON.stringify(response.data),
        };
    }
}

module.exports = ListEventStreamsHandler;