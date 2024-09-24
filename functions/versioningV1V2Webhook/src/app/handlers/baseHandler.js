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
        
        let xPagopaPnApiVersion = "v" + version;
        headers["x-pagopa-pn-api-version"] = xPagopaPnApiVersion;

        return headers;
    }
}


module.exports = EventHandler;