package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

class SendDigitalProgressDetailsTest {

    private SendDigitalProgressDetailsV23 details;

    @BeforeEach
    void setUp() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        details = new SendDigitalProgressDetailsV23();
        details.setRecIndex(1);
        details.setDigitalAddress(DigitalAddress.builder().address("add").build());
        details.setSendingReceipts(Collections.singletonList(SendingReceipt.builder().id("001").build()));
        details.setDigitalAddressSource(DigitalAddressSource.GENERAL);
        details.setDeliveryDetailCode("001");
        details.setDeliveryFailureCause("C1");
        details.setRetryNumber(1);
        details.setShouldRetry(Boolean.TRUE);
        details.setNotificationDate(instant);
    }

    @Test
    void recIndex() {
        SendDigitalProgressDetailsV23 expected = buildSendDigitalProgressDetails();

        SendDigitalProgressDetailsV23 actual = details.recIndex(1);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void eventCode() {
        SendDigitalProgressDetailsV23 expected = buildSendDigitalProgressDetails();

        SendDigitalProgressDetailsV23 actual = details.deliveryDetailCode("001");

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getEventCode() {
        Assertions.assertEquals("001", details.getDeliveryDetailCode());
    }

    @Test
    void shouldRetry() {
        SendDigitalProgressDetailsV23 expected = buildSendDigitalProgressDetails();

        SendDigitalProgressDetailsV23 actual = details.shouldRetry(Boolean.TRUE);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getShouldRetry() {
        Assertions.assertEquals(Boolean.TRUE, details.getShouldRetry());
    }

    @Test
    void digitalAddress() {
        SendDigitalProgressDetailsV23 expected = buildSendDigitalProgressDetails();

        SendDigitalProgressDetailsV23 actual = details.digitalAddress(DigitalAddress.builder().address("add").build());

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getDigitalAddress() {
        Assertions.assertEquals(DigitalAddress.builder().address("add").build(), details.getDigitalAddress());
    }

    @Test
    void digitalAddressSource() {
        SendDigitalProgressDetailsV23 expected = buildSendDigitalProgressDetails();

        SendDigitalProgressDetailsV23 actual = details.digitalAddressSource(DigitalAddressSource.GENERAL);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getDigitalAddressSource() {
        Assertions.assertEquals(DigitalAddressSource.GENERAL, details.getDigitalAddressSource());
    }

    @Test
    void notificationDate() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        SendDigitalProgressDetailsV23 expected = buildSendDigitalProgressDetails();

        SendDigitalProgressDetailsV23 actual = details.notificationDate(instant);

        Assertions.assertEquals(expected, actual);

    }

    @Test
    void getNotificationDate() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        Assertions.assertEquals(instant, details.getNotificationDate());
    }

    @Test
    void sendingReceipts() {
        SendDigitalProgressDetailsV23 expected = buildSendDigitalProgressDetails();

        SendDigitalProgressDetailsV23 actual = details.sendingReceipts(Collections.singletonList(SendingReceipt.builder().id("001").build()));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getSendingReceipts() {
        Assertions.assertEquals(Collections.singletonList(SendingReceipt.builder().id("001").build()), details.getSendingReceipts());
    }

    @Test
    void retryNumber() {
        SendDigitalProgressDetailsV23 expected = buildSendDigitalProgressDetails();

        SendDigitalProgressDetailsV23 actual = details.retryNumber(1);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getRetryNumber() {
        Assertions.assertEquals(1, details.getRetryNumber());
    }

    @Test
    void testEquals() {
        SendDigitalProgressDetailsV23 data = buildSendDigitalProgressDetails();
        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }

    private SendDigitalProgressDetailsV23 buildSendDigitalProgressDetails() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        return SendDigitalProgressDetailsV23.builder()
                .recIndex(1)
                .retryNumber(1)
                .sendingReceipts(Collections.singletonList(SendingReceipt.builder().id("001").build()))
                .digitalAddress(DigitalAddress.builder().address("add").build())
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .deliveryDetailCode("001")
                .deliveryFailureCause("C1")
                .shouldRetry(Boolean.TRUE)
                .notificationDate(instant)
                .build();
    }
}