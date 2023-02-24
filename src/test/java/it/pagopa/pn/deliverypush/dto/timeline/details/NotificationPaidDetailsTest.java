package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class NotificationPaidDetailsTest {

    private NotificationPaidDetails details;

    @BeforeEach
    void setUp() {
        details = new NotificationPaidDetails();
        details.setRecIndex(1);
        details.setAmount(1000);
        details.setNoticeCode("noticeCode");
        details.setCreditorTaxId("creditorTaxId");
        details.setPaymentDate(Instant.now());
        details.setPaymentSourceChannel("source");
    }

    @Test
    void toLog() {

        Assertions.assertEquals(details.toLog(), details.toLog());
    }
}