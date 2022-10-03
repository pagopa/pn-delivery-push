package it.pagopa.pn.deliverypush.dto.ext.datavault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RecipientTypeIntTest {

    @Test
    void getValue() {
        String value = RecipientTypeInt.PF.getValue();
        Assertions.assertEquals("PF", value);
    }

    @Test
    void values() {
        RecipientTypeInt[] recipientTypeInts = RecipientTypeInt.values();
        Assertions.assertEquals(2, recipientTypeInts.length);
    }

    @Test
    void valueOf() {
        RecipientTypeInt recipientTypeInt = RecipientTypeInt.valueOf("PF");
        Assertions.assertEquals("PF", recipientTypeInt.getValue());
    }
}