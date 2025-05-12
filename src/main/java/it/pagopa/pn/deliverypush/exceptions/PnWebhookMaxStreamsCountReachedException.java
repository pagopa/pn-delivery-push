package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;

public class PnWebhookMaxStreamsCountReachedException extends PnRuntimeException {

    static final String MESSAGE = "Max streams count reached for PA";

    public PnWebhookMaxStreamsCountReachedException() {
        super(MESSAGE, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_MAXSTREAMSCOUNTREACHED, 409, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_MAXSTREAMSCOUNTREACHED, null, MESSAGE);
    }

}
