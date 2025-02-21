class EventHandler {

    baseUrl;
    attemptTimeout;
    numRetry;
    constructor() {
        this.baseUrl = process.env.PN_WEBHOOK_URL;
        this.attemptTimeout = process.env.ATTEMPT_TIMEOUT_SEC * 1000;
        this.numRetry = process.env.NUM_RETRY;
    }

    checkOwnership(event, context){}

    handlerEvent(event, context){}

    prepareHeaders(event, version){

        const headers = event["headers"];
        headers["x-pagopa-pn-src-ch"] = "B2B";

        if (event.requestContext.authorizer["cx_groups"]) {
            headers["x-pagopa-pn-cx-groups"] =
                event.requestContext.authorizer["cx_groups"];
        }
        if (event.requestContext.authorizer["cx_id"]) {
            headers["x-pagopa-pn-cx-id"] = event.requestContext.authorizer["cx_id"];
        }
        if (event.requestContext.authorizer["cx_role"]) {
            headers["x-pagopa-pn-cx-role"] = event.requestContext.authorizer["cx_role"];
        }
        if (event.requestContext.authorizer["cx_type"]) {
            headers["x-pagopa-pn-cx-type"] = event.requestContext.authorizer["cx_type"];
        }
        if (event.requestContext.authorizer["cx_jti"]) {
            headers["x-pagopa-pn-jti"] = event.requestContext.authorizer["cx_jti"];
        }
        if (event.requestContext.authorizer["sourceChannelDetails"]) {
            headers["x-pagopa-pn-src-ch-details"] =
                event.requestContext.authorizer["sourceChannelDetails"];
        }
        if (event.requestContext.authorizer["uid"]) {
            headers["x-pagopa-pn-uid"] = event.requestContext.authorizer["uid"];
        }
        
        let xPagopaPnApiVersion = 'v' + version;
        headers["x-pagopa-pn-api-version"] = xPagopaPnApiVersion;

        return headers;
    }

    getVersion(event){
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

        if (event["path"].includes("v2.5")) {
            version = 25;
        }

        if (event["path"].includes("v2.6")) {
            version = 26;
        }

        console.log('version is ', version);

        return version;
    }
}


module.exports = EventHandler;