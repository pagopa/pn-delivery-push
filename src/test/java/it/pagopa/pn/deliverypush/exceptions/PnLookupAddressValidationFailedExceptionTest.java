package it.pagopa.pn.deliverypush.exceptions;

import org.junit.jupiter.api.Test;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PnLookupAddressValidationFailedExceptionTest {

    @Test
    public void testExceptionMessageAndDetails() {
        ProblemError error = new ProblemError();
        error.setCode(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_NOT_FOUND.getValue());
        error.setDetail("Validazione fallita. Problemi nella ricerca sui registri pubblici");
        List<ProblemError> errors = List.of(error);
        PnLookupAddressValidationFailedException exception = new PnLookupAddressValidationFailedException(errors);

        assertNotNull(exception);
        assertNotNull(exception.getProblem());
        assertEquals("ADDRESS_NOT_FOUND", exception.getProblem().getErrors().get(0).getCode());
        assertEquals("Validazione fallita. Problemi nella ricerca sui registri pubblici", exception.getProblem().getDetail());
    }

}
