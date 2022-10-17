package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleAnalogWorkflowDetailsIntTest {

    private ScheduleAnalogWorkflowDetailsInt detailsInt;

    @BeforeEach
    void setUp() {
        detailsInt = new ScheduleAnalogWorkflowDetailsInt();
        detailsInt.setRecIndex(1);
    }

    @Test
    void toLog() {
        Assertions.assertEquals("recIndex=1", detailsInt.toLog());
    }

}