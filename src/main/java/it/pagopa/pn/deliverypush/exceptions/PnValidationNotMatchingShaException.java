package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRefusedErrorCode;

import java.util.List;

public class PnValidationNotMatchingShaException extends PnValidationException {

    public PnValidationNotMatchingShaException(NotificationRefusedErrorCode errorCode, String detail) {
        super("Validazione fallita, sha256 non congruente", List.of(ProblemError.builder()
                .code(errorCode.getValue())
                .detail(detail)
                .build()), null );
    }

}
