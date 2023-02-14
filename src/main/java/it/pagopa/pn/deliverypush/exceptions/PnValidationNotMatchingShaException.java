package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationNotMatchingShaException extends PnValidationException {

    public PnValidationNotMatchingShaException(String detail) {
        super("Validazione fallita, sha256 non congruente",
                List.of(ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_SHA_ERROR.getValue())
                .detail(detail)
                .build()), null );
    }

}
