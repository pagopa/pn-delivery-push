package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class StreamListElementTest {

    private StreamListElement element;

    @BeforeEach
    void setUp() {
        element = new StreamListElement();
        element.setStreamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"));
        element.setTitle("001");
    }

    @Test
    void streamId() {
        StreamListElement expected = StreamListElement.builder()
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .title("001")
                .build();
        Assertions.assertEquals(expected, element.streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454")));
    }

    @Test
    void getStreamId() {
        Assertions.assertEquals(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"), element.getStreamId());
    }

    @Test
    void title() {
        StreamListElement expected = StreamListElement.builder()
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .title("001")
                .build();
        Assertions.assertEquals(expected, element.title("001"));
    }

    @Test
    void getTitle() {
        Assertions.assertEquals("001", element.getTitle());
    }

    @Test
    void testEquals() {
        StreamListElement expected = StreamListElement.builder()
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .title("001")
                .build();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(element));
    }
}