package it.pagopa.pn.deliverypush.utils;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategoryV20;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElementV20;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsIdV20;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LegalFactUtilsTest {

    @Test
    void checkLegalFactConversion() {
        //GIVEN
        LegalFactListElementV20 legalFact = new LegalFactListElementV20();
        legalFact.setIun("iun");
        LegalFactsIdV20 id = new LegalFactsIdV20();
        id.setKey("key");
        id.setCategory(LegalFactCategoryV20.SENDER_ACK);
        legalFact.setLegalFactsId(id);
        legalFact.setTaxId("taxId");

        // WHEN
        LegalFactListElementV20 convertedLegalFact = LegalFactUtils.convert(legalFact);

        // THEN
        Assertions.assertEquals(legalFact, convertedLegalFact);

    }
}
