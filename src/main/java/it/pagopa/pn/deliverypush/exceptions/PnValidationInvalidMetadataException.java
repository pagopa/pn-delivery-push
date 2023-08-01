package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationInvalidMetadataException extends PnValidationException {

    public PnValidationInvalidMetadataException(String detail, Throwable ex) {
        super( detail , 
                List.of(
                        ProblemError.builder()
                                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.INVALID_F24_METADATA.getValue())
                                .detail(detail)
                                .build()
                ), 
                ex 
        );
    }

}
