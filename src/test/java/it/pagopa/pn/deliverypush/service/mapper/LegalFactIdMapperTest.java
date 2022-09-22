package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LegalFactIdMapperTest {

    @Test
    void internalToExternal() {

        LegalFactsId actual = LegalFactIdMapper.internalToExternal(buildLegalFactsIdInt());

        Assertions.assertEquals(buildLegalFactsId(), actual);

    }

    private LegalFactsId buildLegalFactsId() {
        return LegalFactsId.builder()
                .key("001")
                .category(LegalFactCategory.ANALOG_DELIVERY)
                .build();
    }

    private LegalFactsIdInt buildLegalFactsIdInt() {
        return LegalFactsIdInt.builder()
                .key("001")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build();
    }
}