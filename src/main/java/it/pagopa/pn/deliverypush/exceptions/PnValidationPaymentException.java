package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationPaymentException extends PnValidationException {

    public PnValidationPaymentException(String detail) {
        super("Validation failed, payment not valid",
                List.of(ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.PAYMENT_NOT_VALID.getValue())
                .detail(detail)
                .build()), null );
    }
}
