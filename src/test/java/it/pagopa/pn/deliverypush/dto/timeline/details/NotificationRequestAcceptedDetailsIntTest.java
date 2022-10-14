package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationRequestAcceptedDetailsIntTest {

    private NotificationRequestAcceptedDetailsInt detailsInt;

    @BeforeEach
    void setUp() {
        detailsInt = new NotificationRequestAcceptedDetailsInt();
    }

    @Test
    void toLog() {
        Assertions.assertEquals("empty", detailsInt.toLog());
    }

}