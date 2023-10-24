package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_PAYMENT_UPDATE_RETRY_EXCEPTION;

public class PnPaymentUpdateRetryException extends PnInternalException {
    public PnPaymentUpdateRetryException(String message) {
        super(message, ERROR_CODE_DELIVERYPUSH_PAYMENT_UPDATE_RETRY_EXCEPTION);
    }
}
