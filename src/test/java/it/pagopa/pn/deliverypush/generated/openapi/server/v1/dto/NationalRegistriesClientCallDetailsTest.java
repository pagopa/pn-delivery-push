package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class NationalRegistriesClientCallDetailsTest {

    private PublicRegistryCallDetails details;

    @BeforeEach
    void setUp() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        details = new PublicRegistryCallDetails();
        details.setRecIndex(1);
        details.setContactPhase(ContactPhase.SEND_ATTEMPT);
        details.setSendDate(instant);
        details.setDeliveryMode(DeliveryMode.DIGITAL);
        details.sentAttemptMade(1);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void getDeliveryMode() {
        Assertions.assertEquals(DeliveryMode.DIGITAL, details.getDeliveryMode());
    }

    @Test
    void getContactPhase() {
        Assertions.assertEquals(ContactPhase.SEND_ATTEMPT, details.getContactPhase());
    }

    @Test
    void getSentAttemptMade() {
        Assertions.assertEquals(1, details.getSentAttemptMade());
    }

    @Test
    void getSendDate() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        Assertions.assertEquals(instant, details.getSendDate());

    }

    @Test
    void testEquals() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        PublicRegistryCallDetails data = PublicRegistryCallDetails.builder()
                .recIndex(1)
                .sentAttemptMade(1)
                .contactPhase(ContactPhase.SEND_ATTEMPT)
                .deliveryMode(DeliveryMode.DIGITAL)
                .sendDate(instant)
                .build();

        Assertions.assertEquals(data, details);
    }

    @Test
    void testToString() {
        String data = "class PublicRegistryCallDetails {\n" +
                "    recIndex: 1\n" +
                "    deliveryMode: DIGITAL\n" +
                "    contactPhase: SEND_ATTEMPT\n" +
                "    sentAttemptMade: 1\n" +
                "    sendDate: 2021-09-16T15:23:00Z\n" +
                "}";
        Assertions.assertEquals(data, details.toString());
    }
}