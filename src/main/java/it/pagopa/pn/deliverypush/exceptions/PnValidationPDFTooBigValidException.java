package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnValidationPDFTooBigValidException extends PnValidationException {

    public PnValidationPDFTooBigValidException(String detail) {
        super("Validazione fallita, pdf troppo grande",
                List.of(ProblemError.builder()
                .code(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_PDF_TOOBIG_ERROR.getValue())
                .detail(detail)
                .build()), null );
    }

}
