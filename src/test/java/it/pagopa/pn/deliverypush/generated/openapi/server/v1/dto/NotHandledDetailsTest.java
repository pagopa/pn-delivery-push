package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotHandledDetailsTest {

    private NotHandledDetails details;

    @BeforeEach
    void setUp() {
        details = new NotHandledDetails();
        details.setRecIndex(1);
        details.setReason("reason");
        details.setReasonCode("code");
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void getReasonCode() {
        Assertions.assertEquals("code", details.getReasonCode());
    }

    @Test
    void getReason() {
        Assertions.assertEquals("reason", details.getReason());
    }

    @Test
    void testEquals() {
        NotHandledDetails data = NotHandledDetails.builder()
                .reason("reason")
                .reasonCode("code")
                .recIndex(1)
                .build();

        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }
}