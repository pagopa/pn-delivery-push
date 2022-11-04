package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;


//FIXME Viene creata exception specifica perchè Validation è protected MA supera il massimo di parent
public class PnValidationFileNotFoundException extends PnValidationException {

    public PnValidationFileNotFoundException(String errorCode, String detail, Throwable ex) {
        super("Validazione fallita, file non trovato su safe-storage", List.of(ProblemError.builder()
                .code(errorCode)
                .detail(detail)
                .build()), ex );
    }

}
