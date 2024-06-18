package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;

public class PnWebhookMaxStreamsCountReachedException extends PnRuntimeException {

    final static String message = "Max streams count reached for PA";

    public PnWebhookMaxStreamsCountReachedException() {
        super(message, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_MAXSTREAMSCOUNTREACHED, 409, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_MAXSTREAMSCOUNTREACHED, null, message);
    }

}
