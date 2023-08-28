package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CancelledDetailsIntTest {

    private CancelledDetailsInt detailsInt;
    @BeforeEach
    void setUp() {
        detailsInt = new CancelledDetailsInt();
        detailsInt.setNotificationCost(50);
        detailsInt.setNotRefinedRecipientIndexes(new int[]{1,2});
    }

    @Test
    void toLog() {
        Assertions.assertEquals("notificationCost=50 notRefinedRecipientIndexes=[1, 2]", detailsInt.toLog());
    }

    @Test
    void checkNotRefinedRecipientIndexes(){
        Assertions.assertEquals(2, detailsInt.getNotRefinedRecipientIndexes().length);
        Assertions.assertEquals(1, detailsInt.getNotRefinedRecipientIndexes()[0]);
        Assertions.assertEquals(2, detailsInt.getNotRefinedRecipientIndexes()[1]);
    }
}
