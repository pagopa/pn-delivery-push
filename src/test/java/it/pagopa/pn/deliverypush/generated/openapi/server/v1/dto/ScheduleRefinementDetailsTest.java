package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleRefinementDetailsTest {

    private ScheduleRefinementDetails details;

    @BeforeEach
    public void setup() {
        details = ScheduleRefinementDetails.builder()
                .recIndex(0)
                .build();
    }


    @Test
    void getRecIndex() {
        int rec = details.getRecIndex();
        Assertions.assertEquals(0, rec);
    }


    @Test
    void testEquals() {
        ScheduleRefinementDetails tmp = ScheduleRefinementDetails.builder()
                .recIndex(0)
                .build();
        Boolean isEqual = tmp.equals(details);
        Assertions.assertEquals(Boolean.TRUE, isEqual);
    }


}