package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SendAnalogTimeoutDetailsIntTest {

    @Test
    void testGettersAndSetters() {
        Instant now = Instant.now();
        SendAnalogTimeoutDetailsInt details = new SendAnalogTimeoutDetailsInt();
        details.setTimeoutDate(now);

        assertEquals(now, details.getTimeoutDate());
    }

    @Test
    void testToLog() {
        Instant timeout = Instant.parse("2024-06-01T12:00:00Z");
        SendAnalogTimeoutDetailsInt details = SendAnalogTimeoutDetailsInt.builder()
                .recIndex(1)
                .sentAttemptMade(2)
                .relatedRequestId("req-123")
                .timeoutDate(timeout)
                .build();

        String log = details.toLog();
        assertTrue(log.contains("recIndex=1"));
        assertTrue(log.contains("sentAttemptMade=2"));
        assertTrue(log.contains("relatedRequestId=req-123"));
        assertTrue(log.contains("timeoutDate=2024-06-01T12:00:00Z"));
    }

    @Test
    void testGetElementTimestamp() {
        Instant timeout = Instant.now();
        SendAnalogTimeoutDetailsInt details = new SendAnalogTimeoutDetailsInt();
        details.setTimeoutDate(timeout);

        assertEquals(timeout, details.getElementTimestamp());
    }

    @Test
    void testBuilderAndToString() {
        Instant timeout = Instant.now();
        SendAnalogTimeoutDetailsInt details = SendAnalogTimeoutDetailsInt.builder()
                .timeoutDate(timeout)
                .build();

        assertNotNull(details.toString());
        assertEquals(timeout, details.getTimeoutDate());
    }
}
