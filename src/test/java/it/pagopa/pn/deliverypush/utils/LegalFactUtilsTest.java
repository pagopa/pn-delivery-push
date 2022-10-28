package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LegalFactUtilsTest {

    @Test
    void checkLegalFactConversion() {
        //GIVEN
        LegalFactListElement legalFact = new LegalFactListElement();
        legalFact.setIun("iun");
        LegalFactsId id = new LegalFactsId();
        id.setKey("key");
        id.setCategory(LegalFactCategory.SENDER_ACK);
        legalFact.setLegalFactsId(id);
        legalFact.setTaxId("taxId");

        // WHEN
        LegalFactListElement convertedLegalFact = LegalFactUtils.convert(legalFact);

        // THEN
        Assertions.assertEquals(legalFact, convertedLegalFact);

    }
}
