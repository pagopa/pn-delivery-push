package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationRequestAcceptedDetailsTest {

    private NotificationRequestAcceptedDetailsV27 details;

    @BeforeEach
    void setUp() {
        details = new NotificationRequestAcceptedDetailsV27();
        details.setRecIndex(1);
        details.setNotificationRequestId("notificationRequestId");
        details.setPaProtocolNumber("paProtocolNumber");
        details.setIdempotenceToken("idempotenceToken");
    }

    @Test
    void verifyFields() {
        Assertions.assertEquals(1, details.getRecIndex());
        Assertions.assertEquals("notificationRequestId", details.getNotificationRequestId());
        Assertions.assertEquals("paProtocolNumber", details.getPaProtocolNumber());
        Assertions.assertEquals("idempotenceToken", details.getIdempotenceToken());
    }

    @Test
    void testEquals() {
        NotificationRequestAcceptedDetailsV27 tmp = NotificationRequestAcceptedDetailsV27.builder()
                .recIndex(1)
                .notificationRequestId("notificationRequestId")
                .paProtocolNumber("paProtocolNumber")
                .idempotenceToken("idempotenceToken")
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }

}