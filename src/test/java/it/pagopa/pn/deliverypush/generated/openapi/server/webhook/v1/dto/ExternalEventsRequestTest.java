package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class ExternalEventsRequestTest {

    private ExternalEventsRequest eventsRequest;

    @BeforeEach
    void setUp() {
        eventsRequest = new ExternalEventsRequest();
        eventsRequest.setEvents(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build()));
    }

    @Test
    void events() {
        ExternalEventsRequest expected = ExternalEventsRequest.builder()
                .events(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build()))
                .build();

        Assertions.assertEquals(expected, eventsRequest.events(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build())));
    }

    @Test
    void getEvents() {
        Assertions.assertEquals(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build()), eventsRequest.getEvents());
    }

    @Test
    void testEquals() {
        ExternalEventsRequest expected = ExternalEventsRequest.builder()
                .events(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build()))
                .build();

        Assertions.assertEquals(Boolean.TRUE, expected.equals(eventsRequest));
    }

    @Test
    void testToString() {
        String expected = "class ExternalEventsRequest {\n" +
                "    events: [class ExternalEvent {\n" +
                "        payment: class PaymentEvent {\n" +
                "            iun: 001\n" +
                "            recipientTaxId: null\n" +
                "            recipientType: null\n" +
                "            paymentType: null\n" +
                "            timestamp: null\n" +
                "        }\n" +
                "    }]\n" +
                "}";
        Assertions.assertEquals(expected, eventsRequest.toString());
    }
}