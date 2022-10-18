package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CxTypeAuthFleetTest {

    @Test
    void testToString() {

        String actual = CxTypeAuthFleet.PA.toString();
        Assertions.assertEquals("PA", actual);
    }

    @Test
    void fromValue() {
        CxTypeAuthFleet actual = CxTypeAuthFleet.fromValue("PA");
        Assertions.assertEquals(CxTypeAuthFleet.PA, actual);
    }
}