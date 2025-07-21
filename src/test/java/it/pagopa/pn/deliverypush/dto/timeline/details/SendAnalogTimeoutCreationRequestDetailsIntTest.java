package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SendAnalogTimeoutCreationRequestDetailsIntTest {

    @Test
    void testAllArgsConstructorAndGetters() {
        Instant now = Instant.now();

        SendAnalogTimeoutCreationRequestDetailsInt details = SendAnalogTimeoutCreationRequestDetailsInt.builder()
                .timeoutDate(now)
                .recIndex(1)
                .sentAttemptMade(2)
                .relatedRequestId("reqId")
                .legalFactId("factId")
                .build();

        assertEquals(now, details.getTimeoutDate());
        assertEquals(1, details.getRecIndex());
        assertEquals(2, details.getSentAttemptMade());
        assertEquals("reqId", details.getRelatedRequestId());
        assertEquals("factId", details.getLegalFactId());
    }

    @Test
    void testSetters() {
        SendAnalogTimeoutCreationRequestDetailsInt details = new SendAnalogTimeoutCreationRequestDetailsInt();
        Instant now = Instant.now();

        details.setTimeoutDate(now);
        details.setRecIndex(3);
        details.setSentAttemptMade(4);
        details.setRelatedRequestId("rId");
        details.setLegalFactId("lId");

        assertEquals(now, details.getTimeoutDate());
        assertEquals(3, details.getRecIndex());
        assertEquals(4, details.getSentAttemptMade());
        assertEquals("rId", details.getRelatedRequestId());
        assertEquals("lId", details.getLegalFactId());
    }

    @Test
    void testToLog() {
        Instant now = Instant.parse("2024-06-01T12:00:00Z");
        SendAnalogTimeoutCreationRequestDetailsInt details = SendAnalogTimeoutCreationRequestDetailsInt.builder()
                .timeoutDate(now)
                .recIndex(5)
                .sentAttemptMade(6)
                .relatedRequestId("req123")
                .legalFactId("fact456")
                .build();

        String log = details.toLog();
        assertTrue(log.contains("recIndex=5"));
        assertTrue(log.contains("sentAttemptMade=6"));
        assertTrue(log.contains("relatedRequestId=req123"));
        assertTrue(log.contains("timeoutDate=2024-06-01T12:00:00Z"));
        assertTrue(log.contains("legalFactId=fact456"));
    }

    @Test
    void testGetElementTimestamp() {
        Instant now = Instant.now();
        SendAnalogTimeoutCreationRequestDetailsInt details = SendAnalogTimeoutCreationRequestDetailsInt.builder()
                .timeoutDate(now)
                .build();

        assertEquals(now, details.getElementTimestamp());
    }
}