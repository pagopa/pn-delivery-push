package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DeliveryModeTest {

    @Test
    void getValue() {
        String value = DeliveryMode.DIGITAL.getValue();
        Assertions.assertEquals("DIGITAL", value);
    }

    @Test
    void testToString() {
        String value = DeliveryMode.ANALOG.toString();
        Assertions.assertEquals("ANALOG", value);
    }

    @Test
    void fromValue() {
        DeliveryMode value = DeliveryMode.fromValue("ANALOG");
        Assertions.assertEquals(DeliveryMode.ANALOG, value);
    }

    @Test
    void valueOf() {
        DeliveryMode value = DeliveryMode.valueOf("DIGITAL");
        Assertions.assertEquals(DeliveryMode.DIGITAL, value);
    }
}