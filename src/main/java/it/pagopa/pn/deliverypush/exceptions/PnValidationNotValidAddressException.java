package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationNotValidAddressException extends PnValidationException {

    public PnValidationNotValidAddressException(String detail) {
        super("Validazione fallita, indirizzo non valido",
                List.of(ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.NOT_VALID_ADDRESS.getValue())
                .detail(detail)
                .build()), null );
    }

}
