package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;

public class PnWebhookMaxStreamsCountReachedException extends PnRuntimeException {

    public PnWebhookMaxStreamsCountReachedException() {
        super("Max streams count reached for PA", PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_MAXSTREAMSCOUNTREACHED, 409, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_MAXSTREAMSCOUNTREACHED, null, null);
    }

}
