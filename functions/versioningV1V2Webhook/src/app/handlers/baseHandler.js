class EventHandler {

    baseUrl;
    constructor() {
        this.baseUrl = process.env.PN_WEBHOOK_URL;
    }

    checkOwnership(event, context){}

    handlerEvent(event, context){}

    setHeaders(event){

        const headers = JSON.parse(JSON.stringify(event["headers"]));
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
        headers["x-pagopa-pn-api-version"] = "v10";

        return headers;
    }
}


module.exports = EventHandler;