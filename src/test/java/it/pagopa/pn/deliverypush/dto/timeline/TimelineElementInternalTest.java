package it.pagopa.pn.deliverypush.dto.timeline;

import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

class TimelineElementInternalTest {


    @Test
    void compareToBase() {

        Instant t_primo = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        Instant t_secondo = Instant.EPOCH.plus(2, ChronoUnit.DAYS);

        // caso 1: un xxx con data maggiore va dopo
        TimelineElementInternal t1_progress = TimelineElementInternal.builder()
                .timestamp(t_primo)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .build();

        TimelineElementInternal t2_progress = TimelineElementInternal.builder()
                .timestamp(t_secondo)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .build();


        Assertions.assertTrue(t1_progress.compareTo(t2_progress) < 0);

        // caso 2: un feedback con stessa data, va dopo
        t2_progress= TimelineElementInternal.builder()
                .timestamp(t_primo)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();

        Assertions.assertTrue(t1_progress.compareTo(t2_progress) < 0);

        // caso 3: un progress con data maggiore, va dopo il feedback

        t1_progress = TimelineElementInternal.builder()
                .timestamp(t_secondo)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .build();
        t2_progress= TimelineElementInternal.builder()
                .timestamp(t_primo)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();

        Assertions.assertTrue(t1_progress.compareTo(t2_progress) > 0);
    }

    @Test
    void compareToBaseAnalog() {

        Instant t_primo = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        Instant t_secondo = Instant.EPOCH.plus(2, ChronoUnit.DAYS);

        // caso 1: un xxx con data maggiore va dopo
        TimelineElementInternal t1_progress = TimelineElementInternal.builder()
                .timestamp(t_primo)
                .category(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS)
                .build();

        TimelineElementInternal t2_progress = TimelineElementInternal.builder()
                .timestamp(t_secondo)
                .category(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS)
                .build();


        Assertions.assertTrue(t1_progress.compareTo(t2_progress) < 0);

        // caso 2: un feedback con stessa data, va dopo
        t2_progress= TimelineElementInternal.builder()
                .timestamp(t_primo)
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .build();

        Assertions.assertTrue(t1_progress.compareTo(t2_progress) < 0);

        // caso 3: un progress con data maggiore, va dopo il feedback

        t1_progress = TimelineElementInternal.builder()
                .timestamp(t_secondo)
                .category(TimelineElementCategoryInt.SEND_ANALOG_PROGRESS)
                .build();
        t2_progress= TimelineElementInternal.builder()
                .timestamp(t_primo)
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .build();

        Assertions.assertTrue(t1_progress.compareTo(t2_progress) > 0);
    }


    @Test
    void compareTo() {

        Instant t1 = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        Instant t2 = Instant.EPOCH.plus(2, ChronoUnit.DAYS);

        TimelineElementInternal t1_progress = TimelineElementInternal.builder()
                .timestamp(t1)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .build();

        TimelineElementInternal t2_progress = TimelineElementInternal.builder()
                .timestamp(t2)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .build();

        Set<TimelineElementInternal> set = Set.of(t1_progress, t2_progress);
        List<TimelineElementInternal> list = set.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        Assertions.assertEquals(t1_progress, list.get(0));
        Assertions.assertEquals(t2_progress, list.get(1));

    }

    @Test
    void compareToSame() {

        Instant t1 = Instant.EPOCH.plus(1, ChronoUnit.DAYS);

        TimelineElementInternal t1_progress = TimelineElementInternal.builder()
                .timestamp(t1)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .build();

        TimelineElementInternal t2_progress = TimelineElementInternal.builder()
                .timestamp(t1)
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();

        Set<TimelineElementInternal> set = Set.of(t1_progress, t2_progress);
        List<TimelineElementInternal> list = set.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        Assertions.assertEquals(t1_progress, list.get(0));
        Assertions.assertEquals(t2_progress, list.get(1));

    }
}