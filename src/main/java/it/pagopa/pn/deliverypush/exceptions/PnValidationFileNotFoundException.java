package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRefusedErrorCode;

import java.util.List;

public class PnValidationFileNotFoundException extends PnValidationException {

    public PnValidationFileNotFoundException(NotificationRefusedErrorCode errorCode, String detail, Throwable ex) {
        super( detail , 
                List.of(
                        ProblemError.builder()
                                .code(errorCode.getValue())
                                .detail(detail)
                                .build()
                ), 
                ex 
        );
    }

}
