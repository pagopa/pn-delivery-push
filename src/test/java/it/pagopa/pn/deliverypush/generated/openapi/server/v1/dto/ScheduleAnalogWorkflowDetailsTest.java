package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleAnalogWorkflowDetailsTest {
    private ScheduleAnalogWorkflowDetails details;

    @BeforeEach
    void setUp() {
        details = new ScheduleAnalogWorkflowDetails();
        details.setRecIndex(1);
    }

    @Test
    void recIndex() {
        ScheduleAnalogWorkflowDetails tmp = ScheduleAnalogWorkflowDetails.builder()
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
        ScheduleAnalogWorkflowDetails tmp = ScheduleAnalogWorkflowDetails.builder()
                .recIndex(1)
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }

    @Test
    void testToString() {
        String data = "class ScheduleAnalogWorkflowDetails {\n" +
                "    recIndex: 1\n" +
                "}";
        Assertions.assertEquals(data, details.toString());
    }
}