package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ContactPhaseTest {

    @Test
    void getValue() {
        String value = ContactPhase.CHOOSE_DELIVERY.getValue();
        Assertions.assertEquals("CHOOSE_DELIVERY", value);
    }

    @Test
    void testToString() {
        String value = ContactPhase.SEND_ATTEMPT.toString();
        Assertions.assertEquals("SEND_ATTEMPT", value);
    }

    @Test
    void fromValue() {
        ContactPhase value = ContactPhase.fromValue("SEND_ATTEMPT");
        Assertions.assertEquals(ContactPhase.SEND_ATTEMPT, value);
    }

    @Test
    void valueOf() {
        ContactPhase value = ContactPhase.valueOf("CHOOSE_DELIVERY");
        Assertions.assertEquals(ContactPhase.CHOOSE_DELIVERY, value);
    }
}