package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamMetadataResponseTest {

    private StreamMetadataResponseV23 response;

    @BeforeEach
    void setUp() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        response = new StreamMetadataResponseV23();
        response.setStreamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"));
        response.setActivationDate(instant);
        response.setEventType(StreamMetadataResponseV23.EventTypeEnum.STATUS);
        response.setFilterValues(Collections.singletonList("001"));
        response.setTitle("002");
    }

    @Test
    void title() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        StreamMetadataResponseV23 expected = StreamMetadataResponseV23.builder()
                .title("002")
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .activationDate(instant)
                .eventType(StreamMetadataResponseV23.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(expected, response.title("002"));
    }

    @Test
    void getTitle() {
        Assertions.assertEquals("002", response.getTitle());
    }

    @Test
    void eventType() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        StreamMetadataResponseV23 expected = StreamMetadataResponseV23.builder()
                .title("002")
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .activationDate(instant)
                .eventType(StreamMetadataResponseV23.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(expected, response.eventType(StreamMetadataResponseV23.EventTypeEnum.STATUS));
    }

    @Test
    void getEventType() {
        Assertions.assertEquals(StreamMetadataResponseV23.EventTypeEnum.STATUS, response.getEventType());
    }

    @Test
    void filterValues() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        StreamMetadataResponseV23 expected = StreamMetadataResponseV23.builder()
                .title("002")
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .activationDate(instant)
                .eventType(StreamMetadataResponseV23.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(expected, response.filterValues(Collections.singletonList("001")));
    }

    @Test
    void getFilterValues() {
        Assertions.assertEquals(Collections.singletonList("001"), response.getFilterValues());
    }

    @Test
    void streamId() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        StreamMetadataResponseV23 expected = StreamMetadataResponseV23.builder()
                .title("002")
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .activationDate(instant)
                .eventType(StreamMetadataResponseV23.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(expected, response.streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454")));
    }

    @Test
    void getStreamId() {
        Assertions.assertEquals(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"), response.getStreamId());
    }

    @Test
    void activationDate() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        StreamMetadataResponseV23 expected = StreamMetadataResponseV23.builder()
                .title("002")
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .activationDate(instant)
                .eventType(StreamMetadataResponseV23.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(expected, response.activationDate(instant));
    }

    @Test
    void getActivationDate() {
        Assertions.assertEquals(Instant.parse("2021-09-16T15:23:00.00Z"), response.getActivationDate());
    }

    @Test
    void testEquals() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        StreamMetadataResponseV23 expected = StreamMetadataResponseV23.builder()
                .title("002")
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .activationDate(instant)
                .eventType(StreamMetadataResponseV23.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .build();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(response));
    }
}