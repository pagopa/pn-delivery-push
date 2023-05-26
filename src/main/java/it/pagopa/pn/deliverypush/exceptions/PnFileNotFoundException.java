package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.Getter;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_FILE_NOT_FOUND;

@Getter
public class PnFileNotFoundException extends PnInternalException {

    public PnFileNotFoundException(String message, Throwable cause) {
        super(message, ERROR_CODE_DELIVERYPUSH_FILE_NOT_FOUND, cause);
    }

}
