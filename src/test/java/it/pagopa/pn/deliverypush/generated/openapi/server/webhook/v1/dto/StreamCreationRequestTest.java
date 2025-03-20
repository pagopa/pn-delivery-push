package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamCreationRequestTest {

    private StreamCreationRequestV28 request;

    @BeforeEach
    void setUp() {
        request = new StreamCreationRequestV28();
        request.setEventType(StreamCreationRequestV28.EventTypeEnum.STATUS);
        request.setFilterValues(Collections.singletonList("001"));
        request.setTitle("001");
    }

    @Test
    void title() {
        StreamCreationRequestV28 expected = StreamCreationRequestV28.builder()
                .title("001")
                .eventType(StreamCreationRequestV28.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .groups(Collections.emptyList())
                .build();
        Assertions.assertEquals(expected, request.title("001"));
    }

    @Test
    void getTitle() {
        Assertions.assertEquals("001", request.getTitle());
    }

    @Test
    void eventType() {
        StreamCreationRequestV28 expected = StreamCreationRequestV28.builder()
                .title("001")
                .eventType(StreamCreationRequestV28.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .groups(Collections.emptyList())
                .build();
        Assertions.assertEquals(expected, request.eventType(StreamCreationRequestV28.EventTypeEnum.STATUS));
    }

    @Test
    void getEventType() {
        Assertions.assertEquals(StreamCreationRequestV28.EventTypeEnum.STATUS, request.getEventType());
    }

    @Test
    void filterValues() {
        StreamCreationRequestV28 expected = StreamCreationRequestV28.builder()
                .title("001")
                .eventType(StreamCreationRequestV28.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .groups(Collections.emptyList())
                .build();
        Assertions.assertEquals(expected, request.filterValues(Collections.singletonList("001")));
    }

    @Test
    void getFilterValues() {
        Assertions.assertEquals(Collections.singletonList("001"), request.getFilterValues());
    }


}