package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnLookupAddressNotFoundException extends PnValidationException {

    public PnLookupAddressNotFoundException(List<String> details) {
        super("Validazione fallita, Indirizzo non trovato",
                buildProblemErrors(details), null);
    }

    private static List<ProblemError> buildProblemErrors(List<String> details) {
        return details.stream()
            .map(detail -> ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_NOT_FOUND.getValue())
                .detail(detail)
                .build())
            .toList();
    }
}
