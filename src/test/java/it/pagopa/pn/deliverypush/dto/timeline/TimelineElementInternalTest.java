package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class TimelineElementInternalTest {


    @Test
    void compareToBase() {
        Instant t_primo = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        Instant t_secondo = Instant.EPOCH.plus(2, ChronoUnit.DAYS);

        // caso 1: un xxx con data maggiore va dopo
        TimelineElementInternal t1 = TimelineElementInternal.builder()
                .timestamp(t_primo)
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();

        TimelineElementInternal t2 = TimelineElementInternal.builder()
                .timestamp(t_secondo)
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .build();

        Assertions.assertTrue(t1.compareTo(t2) < 0);
    }
}