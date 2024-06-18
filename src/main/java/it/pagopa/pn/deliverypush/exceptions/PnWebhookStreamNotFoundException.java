package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;

public class PnWebhookStreamNotFoundException extends PnRuntimeException {


    public PnWebhookStreamNotFoundException(String message) {
        super(message, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_NOT_FOUND, 404, PnDeliveryPushExceptionCodes.ERROR_CODE_WEBHOOK_NOT_FOUND, null, message);

    }
}
