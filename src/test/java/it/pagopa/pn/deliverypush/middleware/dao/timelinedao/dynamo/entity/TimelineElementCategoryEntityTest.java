package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TimelineElementCategoryEntityTest {
    @Test
    void checkElement(){
        //Nota il test fallisce probabilmente ci si potrebbe essere dimenticati di aggiungere nell'entity un nuovo valore presente nel Dto interno
        assertDoesNotThrow( ()  ->{
            for (TimelineElementCategoryInt timelineElementCategoryInt : TimelineElementCategoryInt.values()) {
                TimelineElementCategoryEntity.valueOf(timelineElementCategoryInt.getValue());
            }
        });
    }
}