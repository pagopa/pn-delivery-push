package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationTaxIdNotValidException extends PnValidationException {

    public PnValidationTaxIdNotValidException(String detail) {
        super( detail ,
                List.of(ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.TAXID_NOT_VALID.getValue())
                .detail(detail)
                .build())
        );
    }

}
