package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceLevelTest {

    @Test
    void getValue() {
        String value = ServiceLevel.REGISTERED_LETTER_890.getValue();
        Assertions.assertEquals("REGISTERED_LETTER_890", value);
    }

    @Test
    void testToString() {
        String value = ServiceLevel.AR_REGISTERED_LETTER.getValue();
        Assertions.assertEquals("AR_REGISTERED_LETTER", value);
    }

    @Test
    void fromValue() {
        ServiceLevel value = ServiceLevel.fromValue("AR_REGISTERED_LETTER");
        Assertions.assertEquals(ServiceLevel.AR_REGISTERED_LETTER, value);
    }

    @Test
    void valueOf() {
        ServiceLevel value = ServiceLevel.fromValue("REGISTERED_LETTER_890");
        Assertions.assertEquals(ServiceLevel.REGISTERED_LETTER_890, value);
    }
}