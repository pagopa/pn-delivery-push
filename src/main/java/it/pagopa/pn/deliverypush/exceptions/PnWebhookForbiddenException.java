package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;

public class PnWebhookForbiddenException extends PnRuntimeException {

    public PnWebhookForbiddenException(String message) {
        super(message, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_FORBIDDEN, 403, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_FORBIDDEN, null, message);
    }

}
