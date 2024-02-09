package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleAnalogWorkflowDetailsTest {
    private ScheduleAnalogWorkflowDetailsV23 details;

    @BeforeEach
    void setUp() {
        details = new ScheduleAnalogWorkflowDetailsV23();
        details.setRecIndex(1);
    }

    @Test
    void recIndex() {
        ScheduleAnalogWorkflowDetailsV23 tmp = ScheduleAnalogWorkflowDetailsV23.builder()
                .recIndex(1)
                .build();
        Assertions.assertEquals(tmp, details.recIndex(1));
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void testEquals() {
        ScheduleAnalogWorkflowDetailsV23 tmp = ScheduleAnalogWorkflowDetailsV23.builder()
                .recIndex(1)
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
    
}