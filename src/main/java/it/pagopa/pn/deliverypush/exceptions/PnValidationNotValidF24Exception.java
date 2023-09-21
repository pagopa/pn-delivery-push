package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationNotValidF24Exception extends PnValidationException {

    public PnValidationNotValidF24Exception(String detail) {
        super("Validazione fallita, f24 non valido",
                List.of(ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.NOT_VALID_ADDRESS.getValue())
                .detail(detail)
                .build()), null );
    }

    public PnValidationNotValidF24Exception(List<String> details) {
        super("Validazione fallita, f24 non valido",
                details.stream().map(detail -> ProblemError.builder()
                        .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.NOT_VALID_F24.getValue())
                        .detail(detail)
                        .build()).toList(), null);
    }

}
