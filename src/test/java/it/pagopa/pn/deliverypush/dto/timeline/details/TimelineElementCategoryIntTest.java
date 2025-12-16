package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimelineElementCategoryIntTest {
    @Test
    void isKnownCategoryReturnsTrueForKnownCategory() {
        String knownCategory = TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST.getValue();
        assertTrue(TimelineElementCategoryInt.isKnownCategory(knownCategory));
    }

    @Test
    void isKnownCategoryReturnsFalseForUnknownCategory() {
        String unknownCategory = "UNKNOWN_CATEGORY";
        assertFalse(TimelineElementCategoryInt.isKnownCategory(unknownCategory));
    }
}