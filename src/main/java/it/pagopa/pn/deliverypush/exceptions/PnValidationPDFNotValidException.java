package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationPDFNotValidException extends PnValidationException {

    public PnValidationPDFNotValidException(String detail) {
        super("Validazione fallita, pdf non valido",
                List.of(ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_PDF_INVALID_ERROR.getValue())
                .detail(detail)
                .build()), null );
    }

}
