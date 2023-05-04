package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationRecipientIdNotValidException extends PnValidationException {

    public PnValidationRecipientIdNotValidException(String detail) {
        super( detail ,
                List.of(ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.RECIPIENT_ID_NOT_VALID.getValue())
                .detail(detail)
                .build())
        );
    }

}
