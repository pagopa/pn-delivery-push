package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategoryV28;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElementV28;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsIdV28;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LegalFactUtilsTest {

    @Test
    void checkLegalFactConversion() {
        //GIVEN
        LegalFactListElementV28 legalFact = new LegalFactListElementV28();
        legalFact.setIun("iun");
        LegalFactsIdV28 id = new LegalFactsIdV28();
        id.setKey("key");
        id.setCategory(LegalFactCategoryV28.SENDER_ACK);
        legalFact.setLegalFactsId(id);
        legalFact.setTaxId("taxId");

        // WHEN
        LegalFactListElementV28 convertedLegalFact = LegalFactUtils.convert(legalFact);

        // THEN
        Assertions.assertEquals(legalFact, convertedLegalFact);

    }
}
