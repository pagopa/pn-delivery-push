class EventHandler {

    url;

    constructor() {
        //fixme : da controllare
        this.url = process.env.PN_DELIVERY_PUSH_URL;
        //this.url = "https://api.dev.notifichedigitali.it/delivery-push"
    }

    checkOwnership(event, context){}

    handlerEvent(event, context){}

    setHeaders(event, headers){

        if (event["headers"]["x-pagopa-pn-uid"])  //.getParameter("x-pagopa-pn-uid");
            headers["x-pagopa-pn-uid"] = event["headers"]["x-pagopa-pn-uid"];
        if (event["headers"]["x-pagopa-pn-cx-type"])
            headers["x-pagopa-pn-cx-type"] = event["headers"]["x-pagopa-pn-cx-type"];
        if(event["headers"]["x-pagopa-pn-cx-id"])
            headers["x-pagopa-pn-cx-id"] = event["headers"]["x-pagopa-pn-cx-id"];

        if (event["headers"]["streamId"])
            headers["streamId"] = event["headers"]["streamId"];
        if(event["headers"]["lastEventId"])
            headers["lastEventId"] = event["headers"]["lastEventId"];

        headers["x-pagopa-pn-cx-groups"] = null;    //required: false
        headers["x-pagopa-pn-api-version"] = "v10";  //required: false

        return headers;
    }
}


module.exports = EventHandler;