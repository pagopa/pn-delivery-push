package it.pagopa.pn.deliverypush.exceptions;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import org.junit.jupiter.api.Assertions;

public class PnLookupAddressNotFoundExceptionTest {

    @Test
    public void testExceptionMessageAndDetails() {
        List<String> details = Arrays.asList("Detail 1", "Detail 2");
        PnLookupAddressNotFoundException exception = new PnLookupAddressNotFoundException(details);

        assertEquals("Bad Request", exception.getMessage());
        assertNotNull(exception.getProblem());

        ProblemError error = new ProblemError();
        error.setCode(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.ADDRESS_NOT_FOUND.getValue());
        error.setDetail(details.get(0));
        Assertions.assertEquals("ADDRESS_NOT_FOUND", error.getCode());
        Assertions.assertEquals("Detail 1", details.get(0));
    }
}
