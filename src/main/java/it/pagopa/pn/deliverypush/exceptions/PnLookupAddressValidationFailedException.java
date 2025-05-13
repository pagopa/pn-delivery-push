package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnLookupAddressValidationFailedException extends PnValidationException {
    public PnLookupAddressValidationFailedException(List<ProblemError> errors) {
        super("Validazione fallita. Problemi nella ricerca sui registri pubblici",
                errors, null);
    }
}
