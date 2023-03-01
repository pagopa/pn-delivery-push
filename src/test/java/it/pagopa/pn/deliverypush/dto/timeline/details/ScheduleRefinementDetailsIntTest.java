package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduleRefinementDetailsIntTest {

    private ScheduleRefinementDetailsInt detailsInt;

    @BeforeEach
    public void setup() {
        detailsInt = ScheduleRefinementDetailsInt.builder()
                .recIndex(1)
                .build();
    }

    @Test
    void toLog() {
        String log = detailsInt.toLog();
        Assertions.assertEquals("recIndex=1", log);
    }

    @Test
    void getRecIndex() {
        int rec = detailsInt.getRecIndex();
        Assertions.assertEquals(1, rec);
    }
    
}