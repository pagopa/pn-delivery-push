package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalogSuccessWorkflowDetailsIntTest {

    private AnalogSuccessWorkflowDetailsInt analogSuccessWorkflowDetailsInt;

    @BeforeEach
    public void setup() {
        analogSuccessWorkflowDetailsInt = new AnalogSuccessWorkflowDetailsInt();
    }

    @Test
    void toLog() {
        String expected = "recIndex=0 physicalAddress='Sensitive information'";

        String actual = analogSuccessWorkflowDetailsInt.toLog();

        Assertions.assertEquals(expected, actual);
    }
}