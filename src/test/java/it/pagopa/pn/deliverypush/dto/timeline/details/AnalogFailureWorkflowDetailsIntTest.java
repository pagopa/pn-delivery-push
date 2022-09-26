package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalogFailureWorkflowDetailsIntTest {

    AnalogFailureWorkflowDetailsInt analogFailureWorkflowDetailsInt;

    @BeforeEach
    public void setup() {

        analogFailureWorkflowDetailsInt = new AnalogFailureWorkflowDetailsInt();
    }

    @Test
    void toLog() {
        String actual = analogFailureWorkflowDetailsInt.toLog();

        Assertions.assertEquals("recIndex=0", actual);
    }
}