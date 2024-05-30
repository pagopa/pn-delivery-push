const axios = require("axios");
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

        console.log('calling ', url);
        let response;
        let lastError = null;
        for (var i=0; i< this.numRetry; i++) {
            console.log('attempt #',i);
            try {
                response = await axios.delete(url, { headers: headers, timeout: this.attemptTimeout});
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


        const ret = {
            statusCode: response.status,
            body: response.data
        }
        return ret;
    }
}

module.exports = DeleteEventStreamHandler;