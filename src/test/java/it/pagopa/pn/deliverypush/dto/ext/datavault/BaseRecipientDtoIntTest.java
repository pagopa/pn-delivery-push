package it.pagopa.pn.deliverypush.dto.ext.datavault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseRecipientDtoIntTest {

    private BaseRecipientDtoInt baseRecipientDtoInt;

    @BeforeEach
    public void setup() {
        baseRecipientDtoInt = BaseRecipientDtoInt.builder()
                .denomination("001")
                .recipientType(RecipientTypeInt.PF)
                .internalId("002")
                .taxId("003")
                .build();
    }

    @Test
    void getInternalId() {
        String internalId = baseRecipientDtoInt.getInternalId();
        Assertions.assertEquals("002", internalId);
    }

    @Test
    void getTaxId() {
        String taxId = baseRecipientDtoInt.getTaxId();
        Assertions.assertEquals("003", taxId);
    }

    @Test
    void getRecipientType() {
        RecipientTypeInt recipientType = baseRecipientDtoInt.getRecipientType();
        Assertions.assertEquals(RecipientTypeInt.PF, recipientType);
    }

    @Test
    void getDenomination() {
        String denomination = baseRecipientDtoInt.getDenomination();
        Assertions.assertEquals("001", denomination);
    }

    @Test
    void testToString() {
        String expected = "BaseRecipientDtoInt(internalId=002, taxId=003, recipientType=PF, denomination=001)";

        String actual = baseRecipientDtoInt.toString();

        Assertions.assertEquals(expected, actual);
    }
}