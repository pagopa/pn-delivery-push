package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PnConflictException extends PnRuntimeException {

    public PnConflictException(String message, String description, String errorcode) {
        super(message, description, HttpStatus.CONFLICT.value(), errorcode, null, description);
    }
}
