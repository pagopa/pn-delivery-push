package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt;
import java.util.List;

public class PnValidationFileGoneException extends PnValidationException {
    public PnValidationFileGoneException(String detail, Throwable ex) {
        super( detail ,
            List.of(
                ProblemError.builder()
                    .code(NotificationRefusedErrorCodeInt.FILE_GONE.getValue())
                    .detail(detail)
                    .build()
            ),
            ex
        );
    }
}
