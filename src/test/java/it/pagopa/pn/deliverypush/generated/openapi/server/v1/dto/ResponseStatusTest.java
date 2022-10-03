package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResponseStatusTest {

    @Test
    void getValue() {
        String value = ResponseStatus.OK.getValue();
        Assertions.assertEquals("OK", value);
    }

    @Test
    void testToString() {
        String value = ResponseStatus.KO.toString();
        Assertions.assertEquals("KO", value);
    }

    @Test
    void fromValue() {
        ResponseStatus status = ResponseStatus.fromValue("KO");
        Assertions.assertEquals(ResponseStatus.KO, status);
    }

    @Test
    void valueOf() {
        ResponseStatus status = ResponseStatus.valueOf("OK");
        Assertions.assertEquals(ResponseStatus.OK, status);
    }
}