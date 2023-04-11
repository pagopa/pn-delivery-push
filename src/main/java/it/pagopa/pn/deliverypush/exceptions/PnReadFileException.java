package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.Getter;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_READ_FILE_ERROR;

@Getter
public class PnReadFileException extends PnInternalException {

    public PnReadFileException(String message, Throwable cause) {
        super(message, ERROR_CODE_DELIVERYPUSH_READ_FILE_ERROR, cause);
    }

}
