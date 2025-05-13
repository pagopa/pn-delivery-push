package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationRequestAcceptedDetailsIntTest {

    private NotificationRequestAcceptedDetailsInt detailsInt;

    @BeforeEach
    public void setup() {
        detailsInt = NotificationRequestAcceptedDetailsInt.builder()
                .paProtocolNumber("paProtocolNumber")
                .idempotenceToken("idempotenceToken")
                .notificationRequestId("notificationRequestId")
                .build();
    }

    @Test
    void toLog() {
        String log = detailsInt.toLog();
        Assertions.assertEquals("notificationRequestId=notificationRequestId, paProtocolNumber=paProtocolNumber, idempotenceToken=idempotenceToken", log);
    }

}