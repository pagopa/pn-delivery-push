package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationViewedDetailsIntTest {

    private NotificationViewedDetailsInt detailsInt;

    @BeforeEach
    void setUp() {
        detailsInt = new NotificationViewedDetailsInt();
        detailsInt.setRecIndex(1);
    }

    @Test
    void toLog() {
        Assertions.assertEquals("recIndex=1 eventTimestamp=null", detailsInt.toLog());
    }
}