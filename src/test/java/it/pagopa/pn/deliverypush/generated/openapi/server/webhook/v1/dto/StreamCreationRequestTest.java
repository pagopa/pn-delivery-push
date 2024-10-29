package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamCreationRequestTest {

    private StreamCreationRequestV25 request;

    @BeforeEach
    void setUp() {
        request = new StreamCreationRequestV25();
        request.setEventType(StreamCreationRequestV25.EventTypeEnum.STATUS);
        request.setFilterValues(Collections.singletonList("001"));
        request.setTitle("001");
    }

    @Test
    void title() {
        StreamCreationRequestV25 expected = StreamCreationRequestV25.builder()
                .title("001")
                .eventType(StreamCreationRequestV25.EventTypeEnum.STATUS)
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
        StreamCreationRequestV25 expected = StreamCreationRequestV25.builder()
                .title("001")
                .eventType(StreamCreationRequestV25.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .groups(Collections.emptyList())
                .build();
        Assertions.assertEquals(expected, request.eventType(StreamCreationRequestV25.EventTypeEnum.STATUS));
    }

    @Test
    void getEventType() {
        Assertions.assertEquals(StreamCreationRequestV25.EventTypeEnum.STATUS, request.getEventType());
    }

    @Test
    void filterValues() {
        StreamCreationRequestV25 expected = StreamCreationRequestV25.builder()
                .title("001")
                .eventType(StreamCreationRequestV25.EventTypeEnum.STATUS)
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