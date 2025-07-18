package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class AnalogFailureWorkflowTimeoutDetailsIntTest {

    @Test
    void testConstructorAndGetters() {
        Instant now = Instant.now();
        AnalogFailureWorkflowTimeoutDetailsInt details = AnalogFailureWorkflowTimeoutDetailsInt.builder()
                .recIndex(1)
                .generatedAarUrl("http://test.url")
                .notificationCost(100)
                .timeoutDate(now)
                .build();

        assertEquals(1, details.getRecIndex());
        assertEquals("http://test.url", details.getGeneratedAarUrl());
        assertEquals(100, details.getNotificationCost());
        assertEquals(now, details.getTimeoutDate());
    }

    @Test
    void testSetters() {
        AnalogFailureWorkflowTimeoutDetailsInt details = new AnalogFailureWorkflowTimeoutDetailsInt();
        Instant now = Instant.now();

        details.setRecIndex(2);
        details.setGeneratedAarUrl("http://another.url");
        details.setNotificationCost(200);
        details.setTimeoutDate(now);

        assertEquals(2, details.getRecIndex());
        assertEquals("http://another.url", details.getGeneratedAarUrl());
        assertEquals(200, details.getNotificationCost());
        assertEquals(now, details.getTimeoutDate());
    }

    @Test
    void testToLog() {
        Instant now = Instant.parse("2024-06-01T12:00:00Z");
        AnalogFailureWorkflowTimeoutDetailsInt details = AnalogFailureWorkflowTimeoutDetailsInt.builder()
                .recIndex(3)
                .notificationCost(300)
                .timeoutDate(now)
                .build();

        String expected = "recIndex=3, cost=300 timeoutDate=2024-06-01T12:00:00Z";
        assertEquals(expected, details.toLog());
    }

    @Test
    void testGetElementTimestamp() {
        Instant now = Instant.now();
        AnalogFailureWorkflowTimeoutDetailsInt details = AnalogFailureWorkflowTimeoutDetailsInt.builder()
                .timeoutDate(now)
                .build();

        assertEquals(now, details.getElementTimestamp());
    }
}
