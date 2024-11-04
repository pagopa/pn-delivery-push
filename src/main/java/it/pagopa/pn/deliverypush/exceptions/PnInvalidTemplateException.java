package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PnInvalidTemplateException extends PnRuntimeException {

    public PnInvalidTemplateException(String message, String drescription, String errorcode) {
        super(message, drescription, HttpStatus.BAD_REQUEST.value(), errorcode, null, drescription);
    }

}
