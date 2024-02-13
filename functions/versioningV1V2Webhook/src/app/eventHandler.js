const CreateEventStreamHandler  = require("./handlers/createEventStreamHandler.js");
const UpdateEventStreamHandler  = require("./handlers/updateEventStreamHandler.js");
const GetEventStreamHandler = require("./handlers/getEventStreamHandler.js");
const InformOnExternalEventHandler = require("./handlers/informOnExternalEventHandler.js");
const ListEventStreamsHandler = require("./handlers/listEventStreamsHandler.js");
const DeleteEventStreamHandler = require("./handlers/deleteEventStreamHandler.js");
const ConsumeEventStreamHandler  = require("./handlers/consumeEventStreamHandler.js");

const AWSXRay = require("aws-xray-sdk-core");

AWSXRay.captureHTTPsGlobal(require('http'));
AWSXRay.captureHTTPsGlobal(require('https'));
AWSXRay.capturePromise();

const { generateProblem } = require("./lib/utils");

exports.eventHandler = async (event, context) => {

    try{
        const handlers = [];
        handlers.push(new ConsumeEventStreamHandler());
        handlers.push(new CreateEventStreamHandler());
        handlers.push(new UpdateEventStreamHandler());
        handlers.push(new GetEventStreamHandler());
        handlers.push(new ListEventStreamsHandler());
        handlers.push(new DeleteEventStreamHandler());
        handlers.push(new InformOnExternalEventHandler());
        for( let i = 0; i<handlers.length; i++){
            if (handlers[i].checkOwnership(event, context)) {
                    let result = handlers[i].handlerEvent(event, context);
                    return result;
            }
        }
        console.log("ERROR ENDPOINT ERRATO");
        const err = {
            //Nella V10 lo statusCode 502 non è accettato
            statusCode: 500,
            body: generateProblem(500, "ENDPOINT ERRATO")
        };
        return err;

    } catch (e) {
        console.log("PN_GENERIC_ERROR")
        if (e.response) {
            // Nella V10 gli statusCode 403 e 404 non sono accettati
            if (e.response.status === 403 || e.response.status === 404)
                e.response.status = 400;

            const ret = {
                statusCode: e.response.status,
                body: JSON.stringify(e.response.data)
            };
            return ret;
        }
        return {
            statusCode: 500,
            body: generateProblem(500, "PN_GENERIC_ERROR")
        }
    }
}

