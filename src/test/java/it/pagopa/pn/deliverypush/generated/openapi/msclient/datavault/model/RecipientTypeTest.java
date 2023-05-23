package it.pagopa.pn.deliverypush.generated.openapi.msclient.datavault.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RecipientTypeTest {

    @Test
    void getValue() {
        String value = RecipientType.PG.getValue();
        Assertions.assertEquals("PG", value);
    }

    @Test
    void testToString() {
        String value = RecipientType.PF.getValue();
        Assertions.assertEquals("PF", value);
    }

    @Test
    void fromValue() {
        RecipientType value = RecipientType.fromValue("PF");
        Assertions.assertEquals(RecipientType.PF, value);
    }

    @Test
    void valueOf() {
        RecipientType value = RecipientType.valueOf("PG");
        Assertions.assertEquals(RecipientType.PG, value);
    }
}