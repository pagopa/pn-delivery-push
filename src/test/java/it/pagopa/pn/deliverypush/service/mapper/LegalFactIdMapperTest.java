package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategoryV28;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsIdV28;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LegalFactIdMapperTest {

    @Test
    void internalToExternal() {

        LegalFactsIdV28 actual = LegalFactIdMapper.internalToExternal(buildLegalFactsIdInt());

        Assertions.assertEquals(buildLegalFactsId(), actual);

    }

    private LegalFactsIdV28 buildLegalFactsId() {
        return LegalFactsIdV28.builder()
                .key("001")
                .category(LegalFactCategoryV28.ANALOG_DELIVERY)
                .build();
    }

    private LegalFactsIdInt buildLegalFactsIdInt() {
        return LegalFactsIdInt.builder()
                .key("001")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build();
    }
}