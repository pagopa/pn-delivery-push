package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;

class HandleEventUtilsTest {

    @Test
    void handleException() {
        MessageHeaders headers = buildMessageHeaders();
        Exception exception = new Exception();

        HandleEventUtils.handleException(headers, exception);
    }

    @Test
    void mapStandardEventHeader() {

        StandardEventHeader actual = HandleEventUtils.mapStandardEventHeader(buildMessageHeaders());

        Assertions.assertEquals(buildStandardEventHeader(), actual);
    }

    private MessageHeaders buildMessageHeaders() {
        Map<String, Object> map = new HashMap<>();
        map.put(PN_EVENT_HEADER_EVENT_ID, "001");
        map.put(PN_EVENT_HEADER_IUN, "002");
        map.put(PN_EVENT_HEADER_EVENT_TYPE, "003");
        map.put(PN_EVENT_HEADER_CREATED_AT, "2021-09-16T15:23:00.00Z");
        map.put(PN_EVENT_HEADER_PUBLISHER, "005");
        return new MessageHeaders(map);
    }

    private StandardEventHeader buildStandardEventHeader() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        return StandardEventHeader.builder()
                .eventId("001")
                .iun("002")
                .eventType("003")
                .createdAt(instant)
                .publisher("005")
                .build();
    }
}