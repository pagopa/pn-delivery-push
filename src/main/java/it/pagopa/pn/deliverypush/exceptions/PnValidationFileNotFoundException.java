package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRefusedErrorCodeInt;

import java.util.List;

public class PnValidationFileNotFoundException extends PnValidationException {

    public PnValidationFileNotFoundException(NotificationRefusedErrorCodeInt errorCode, String detail, Throwable ex) {
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
