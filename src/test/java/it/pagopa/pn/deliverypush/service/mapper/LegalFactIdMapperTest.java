package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdIntWithRecIndex;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.LegalFactWithRecIndex;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.LegalFactsResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategoryV20;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsIdV20;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalFactIdMapperTest {

    @Test
    void internalToExternal() {

        LegalFactsIdV20 actual = LegalFactIdMapper.internalToExternal(buildLegalFactsIdInt());

        Assertions.assertEquals(buildLegalFactsId(), actual);

    }

    private LegalFactsIdV20 buildLegalFactsId() {
        return LegalFactsIdV20.builder()
                .key("001")
                .category(LegalFactCategoryV20.ANALOG_DELIVERY)
                .build();
    }

    private LegalFactsIdInt buildLegalFactsIdInt() {
        return LegalFactsIdInt.builder()
                .key("001")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build();
    }

    @Test
    void toLegalFactsIdIntWithRecIndex_nullInput_returnsEmptyList() {
        List<LegalFactsIdIntWithRecIndex> result = LegalFactIdMapper.toLegalFactsIdIntWithRecIndex(null);
        assertTrue(CollectionUtils.isEmpty(result));
    }

    @Test
    void toLegalFactsIdIntWithRecIndex_emptyLegalFacts_returnsEmptyList() {
        LegalFactsResponse response = new LegalFactsResponse();
        response.setLegalFacts(Collections.emptyList());
        List<LegalFactsIdIntWithRecIndex> result = LegalFactIdMapper.toLegalFactsIdIntWithRecIndex(response);
        assertTrue(CollectionUtils.isEmpty(result));
    }

    @Test
    void toLegalFactsIdIntWithRecIndex_validLegalFacts_mapsCorrectly() {
        LegalFactWithRecIndex fact = new LegalFactWithRecIndex();
        fact.setKey("testKey");
        fact.setCategory(LegalFactWithRecIndex.CategoryEnum.ANALOG_DELIVERY);
        fact.setRecIndex(2);

        LegalFactsResponse response = new LegalFactsResponse();
        response.setLegalFacts(List.of(fact));

        List<LegalFactsIdIntWithRecIndex> result = LegalFactIdMapper.toLegalFactsIdIntWithRecIndex(response);

        assertEquals(1, result.size());
        LegalFactsIdIntWithRecIndex mapped = result.getFirst();
        assertEquals("testKey", mapped.getKey());
        assertEquals(LegalFactCategoryInt.ANALOG_DELIVERY, mapped.getCategory());
        assertEquals(2, mapped.getRecIndex());
    }
}