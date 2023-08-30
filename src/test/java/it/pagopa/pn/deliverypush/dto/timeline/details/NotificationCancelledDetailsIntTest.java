package it.pagopa.pn.deliverypush.dto.timeline.details;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationCancelledDetailsIntTest {

    private NotificationCancelledDetailsInt detailsInt;
    @BeforeEach
    void setUp() {
        detailsInt = new NotificationCancelledDetailsInt();
        detailsInt.setNotificationCost(50);
        List<Integer> indexes= new ArrayList<>();
        indexes.add(1);
        indexes.add(2);
        detailsInt.setNotRefinedRecipientIndexes(indexes);
    }

    @Test
    void toLog() {
        Assertions.assertEquals("notificationCost=50 notRefinedRecipientIndexes=[1, 2]", detailsInt.toLog());
    }

    @Test
    void checkNotRefinedRecipientIndexes(){
        Assertions.assertEquals(2, detailsInt.getNotRefinedRecipientIndexes().size());
        Assertions.assertEquals(1, detailsInt.getNotRefinedRecipientIndexes().get(0));
        Assertions.assertEquals(2, detailsInt.getNotRefinedRecipientIndexes().get(1));
    }
}
