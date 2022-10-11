package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalEventTest {

    private ExternalEvent externalEvent;

    @BeforeEach
    void setUp() {
        externalEvent = new ExternalEvent();
        externalEvent.setPayment(PaymentEvent.builder()
                .iun("001")
                .build());
    }

    @Test
    void payment() {
        ExternalEvent actual = new ExternalEvent();
        actual.payment(PaymentEvent.builder()
                .iun("001")
                .build());
        Assertions.assertEquals(externalEvent, actual);
    }

    @Test
    void getPayment() {
        PaymentEvent expected = PaymentEvent.builder()
                .iun("001")
                .build();
        Assertions.assertEquals(expected, externalEvent.getPayment());
    }

    @Test
    void testEquals() {
        ExternalEvent expected = new ExternalEvent();
        expected.payment(PaymentEvent.builder()
                .iun("001")
                .build());
        Assertions.assertEquals(Boolean.TRUE, expected.equals(externalEvent));
    }

    @Test
    void testToString() {
        String expected = "class ExternalEvent {\n" +
                "    payment: class PaymentEvent {\n" +
                "        iun: 001\n" +
                "        recipientTaxId: null\n" +
                "        recipientType: null\n" +
                "        paymentType: null\n" +
                "        timestamp: null\n" +
                "    }\n" +
                "}";
        Assertions.assertEquals(expected, externalEvent.toString());
    }
}