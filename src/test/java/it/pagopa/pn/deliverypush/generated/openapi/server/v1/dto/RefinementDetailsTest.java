package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefinementDetailsTest {

    private RefinementDetailsV23 details;

    @BeforeEach
    void setUp() {
        details = new RefinementDetailsV23();
        details.setRecIndex(1);
        details.setNotificationCost(1L);
    }

    @Test
    void recIndex() {
        RefinementDetailsV23 tmp = RefinementDetailsV23.builder()
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
        RefinementDetailsV23 tmp = RefinementDetailsV23.builder()
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
        RefinementDetailsV23 tmp = RefinementDetailsV23.builder()
                .recIndex(1)
                .notificationCost(1L)
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
}