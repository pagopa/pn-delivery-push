package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class RefinementDetailsIntTest {

    private RefinementDetailsInt detailsInt;

    @BeforeEach
    public void setup() {
        detailsInt = new RefinementDetailsInt();

    }

    @Test
    void toLog() {
        String expected = "recIndex=0";

        String actual = detailsInt.toLog();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void build() {

        RefinementDetailsInt actual = RefinementDetailsInt.builder()
                .recIndex(0)
                .notificationCost(0)
                .build();

        Assertions.assertEquals(0, actual.getRecIndex());
        Assertions.assertEquals(0, actual.getNotificationCost());

    }

    @Test
    void toStringTest() {

        String expected = "RefinementDetailsInt(recIndex=0, notificationCost=0, eventTimestamp=1970-01-01T00:00:00.010Z)";

        String actual = RefinementDetailsInt.builder()
                .recIndex(0)
                .notificationCost(0)
                .eventTimestamp(Instant.EPOCH.plusMillis(10))
                .build().toString();

        Assertions.assertEquals(expected, actual);
    }
}