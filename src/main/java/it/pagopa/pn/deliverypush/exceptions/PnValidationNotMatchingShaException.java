package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationNotMatchingShaException extends PnValidationException {

    public PnValidationNotMatchingShaException(String errorCode, String detail) {
        super("Validazione fallita a causa di sha256 non congruente", List.of(ProblemError.builder()
                .code(errorCode)
                .detail(detail)
                .build()), null );
    }

}
