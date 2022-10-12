package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefinementDetailsTest {

    private RefinementDetails details;

    @BeforeEach
    void setUp() {
        details = new RefinementDetails();
        details.setRecIndex(1);
        details.setNotificationCost(1L);
    }

    @Test
    void recIndex() {
        RefinementDetails tmp = RefinementDetails.builder()
                .recIndex(1)
                .notificationCost(1L)
                .build();
        Assertions.assertEquals(tmp, details.recIndex(1));
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void notificationCost() {
        RefinementDetails tmp = RefinementDetails.builder()
                .recIndex(1)
                .notificationCost(1L)
                .build();
        Assertions.assertEquals(tmp, details.notificationCost(1L));
    }

    @Test
    void getNotificationCost() {
        Assertions.assertEquals(1L, details.getNotificationCost());
    }

    @Test
    void testEquals() {
        RefinementDetails tmp = RefinementDetails.builder()
                .recIndex(1)
                .notificationCost(1L)
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }

    @Test
    void testToString() {
        String data = "class RefinementDetails {\n" +
                "    recIndex: 1\n" +
                "    notificationCost: 1\n" +
                "}";
        Assertions.assertEquals(data, details.toString());
    }
}