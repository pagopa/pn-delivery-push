package it.pagopa.pn.deliverypush.exceptions;

import lombok.Getter;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_FILE_NOT_FOUND;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_DESCRIPTION_DELIVERYPUSH_ERROR_FILE_NOT_FOUND;

@Getter
public class PnFileNotFoundException extends PnNotFoundException {

    public PnFileNotFoundException(String message, Throwable cause) {
        super(message, ERROR_DESCRIPTION_DELIVERYPUSH_ERROR_FILE_NOT_FOUND,ERROR_CODE_DELIVERYPUSH_FILE_NOT_FOUND, cause);
    }

}
