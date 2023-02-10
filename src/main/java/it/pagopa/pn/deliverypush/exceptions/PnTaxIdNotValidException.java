package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRefusedErrorCode;

import java.util.List;

public class PnTaxIdNotValidException extends PnValidationException {

    public PnTaxIdNotValidException(NotificationRefusedErrorCode errorCode, String detail) {
        super( detail ,
                List.of(ProblemError.builder()
                .code(errorCode.getValue())
                .detail(detail)
                .build())
        );
    }

}
