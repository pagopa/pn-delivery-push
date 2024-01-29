package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamCreationRequestTest {

    private StreamCreationRequestV23 request;

    @BeforeEach
    void setUp() {
        request = new StreamCreationRequestV23();
        request.setEventType(StreamCreationRequestV23.EventTypeEnum.STATUS);
        request.setFilterValues(Collections.singletonList("001"));
        request.setTitle("001");
    }

    @Test
    void title() {
        StreamCreationRequestV23 expected = StreamCreationRequestV23.builder()
                .title("001")
                .eventType(StreamCreationRequestV23.EventTypeEnum.STATUS)
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
        StreamCreationRequestV23 expected = StreamCreationRequestV23.builder()
                .title("001")
                .eventType(StreamCreationRequestV23.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(expected, request.eventType(StreamCreationRequestV23.EventTypeEnum.STATUS));
    }

    @Test
    void getEventType() {
        Assertions.assertEquals(StreamCreationRequestV23.EventTypeEnum.STATUS, request.getEventType());
    }

    @Test
    void filterValues() {
        StreamCreationRequestV23 expected = StreamCreationRequestV23.builder()
                .title("001")
                .eventType(StreamCreationRequestV23.EventTypeEnum.STATUS)
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
        StreamCreationRequestV23 expected = StreamCreationRequestV23.builder()
                .title("001")
                .eventType(StreamCreationRequestV23.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(request));
    }
}