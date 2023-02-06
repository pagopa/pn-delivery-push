package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnTaxIdNotValidException extends PnValidationException {

    public PnTaxIdNotValidException(String errorCode, String detail) {
        super( detail ,
                List.of(ProblemError.builder()
                .code(errorCode)
                .detail(detail)
                .build())
        );
    }

}
