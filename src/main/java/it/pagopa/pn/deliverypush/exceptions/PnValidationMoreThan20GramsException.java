package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

/**
 *
 * quickWorkAroundForPNXYZ
 */
public class PnValidationMoreThan20GramsException extends PnValidationException {
    public PnValidationMoreThan20GramsException(String detail) {
        super("Validazione fallita, mittente non puó inviare piú di 20 grammi di allegati",
                List.of(ProblemError.builder()
                        .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.SENDER_DISABLED_MORE_THAN_20_GRAMS.getValue())
                        .detail(detail)
                        .build()), null );
    }
}
