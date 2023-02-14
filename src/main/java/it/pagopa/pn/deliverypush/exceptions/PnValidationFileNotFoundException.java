package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationFileNotFoundException extends PnValidationException {

    public PnValidationFileNotFoundException(String detail, Throwable ex) {
        super( detail , 
                List.of(
                        ProblemError.builder()
                                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue())
                                .detail(detail)
                                .build()
                ), 
                ex 
        );
    }

}
