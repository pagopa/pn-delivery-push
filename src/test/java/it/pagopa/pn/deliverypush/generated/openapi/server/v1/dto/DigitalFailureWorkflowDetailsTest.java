package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DigitalFailureWorkflowDetailsTest {

    private DigitalFailureWorkflowDetails details;

    @BeforeEach
    void setUp() {
        details = new DigitalFailureWorkflowDetails();
        details.setRecIndex(1);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void testEquals() {
        DigitalFailureWorkflowDetails data = DigitalFailureWorkflowDetails.builder()
                .recIndex(1)
                .build();

        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }
}