package it.pagopa.pn.deliverypush.exceptions;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_FILE_GONE;

import it.pagopa.pn.commons.exceptions.PnInternalException;

public class PnFileGoneException extends PnInternalException {

    public PnFileGoneException(String message, Throwable cause) {
        super(message, ERROR_CODE_DELIVERYPUSH_FILE_GONE, cause);
    }
}
