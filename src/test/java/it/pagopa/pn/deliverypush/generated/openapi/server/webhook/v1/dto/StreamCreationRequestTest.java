package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class StreamCreationRequestTest {

    private StreamCreationRequest request;

    @BeforeEach
    void setUp() {
        request = new StreamCreationRequest();
        request.setEventType(StreamCreationRequest.EventTypeEnum.STATUS);
        request.setFilterValues(Collections.singletonList("001"));
        request.setTitle("001");
    }

    @Test
    void title() {
        StreamCreationRequest expected = StreamCreationRequest.builder()
                .title("001")
                .eventType(StreamCreationRequest.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(expected, request.title("001"));
    }

    @Test
    void getTitle() {
        Assertions.assertEquals("001", request.getTitle());
    }

    @Test
    void eventType() {
        StreamCreationRequest expected = StreamCreationRequest.builder()
                .title("001")
                .eventType(StreamCreationRequest.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(expected, request.eventType(StreamCreationRequest.EventTypeEnum.STATUS));
    }

    @Test
    void getEventType() {
        Assertions.assertEquals(StreamCreationRequest.EventTypeEnum.STATUS, request.getEventType());
    }

    @Test
    void filterValues() {
        StreamCreationRequest expected = StreamCreationRequest.builder()
                .title("001")
                .eventType(StreamCreationRequest.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(expected, request.filterValues(Collections.singletonList("001")));
    }

    @Test
    void getFilterValues() {
        Assertions.assertEquals(Collections.singletonList("001"), request.getFilterValues());
    }

    @Test
    void testEquals() {
        StreamCreationRequest expected = StreamCreationRequest.builder()
                .title("001")
                .eventType(StreamCreationRequest.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(request));
    }

    @Test
    void testToString() {
        String expected = "class StreamCreationRequest {\n" +
                "    title: 001\n" +
                "    eventType: STATUS\n" +
                "    filterValues: [001]\n" +
                "}";
        Assertions.assertEquals(expected, request.toString());
    }
}