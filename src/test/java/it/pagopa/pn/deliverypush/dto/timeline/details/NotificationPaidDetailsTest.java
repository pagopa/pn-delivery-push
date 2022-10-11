package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationPaidDetailsTest {

    private NotificationPaidDetails details;

    @BeforeEach
    void setUp() {
        details = new NotificationPaidDetails();
        details.setRecIndex(1);
    }

    @Test
    void toLog() {
        Assertions.assertEquals("recIndex=1", details.toLog());
    }
}