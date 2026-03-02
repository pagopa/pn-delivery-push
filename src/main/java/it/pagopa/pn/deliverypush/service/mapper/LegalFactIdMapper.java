package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdIntWithRecIndex;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.LegalFactsResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategoryV20;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsIdV20;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class LegalFactIdMapper {
    private LegalFactIdMapper(){}
    
    public static LegalFactsIdV20 internalToExternal(LegalFactsIdInt dtoInt){
        return LegalFactsIdV20.builder()
                .key(dtoInt.getKey())
                .category( dtoInt.getCategory() != null ? LegalFactCategoryV20.fromValue(dtoInt.getCategory().getValue()): null)
                .build();
    }

    public static List<LegalFactsIdIntWithRecIndex> toLegalFactsIdIntWithRecIndex(LegalFactsResponse legalFactsResponse){
        if(legalFactsResponse == null || CollectionUtils.isEmpty(legalFactsResponse.getLegalFacts())) {
            return Collections.emptyList();
        }
        return legalFactsResponse.getLegalFacts().stream()
                .map(legalFact -> (LegalFactsIdIntWithRecIndex) LegalFactsIdIntWithRecIndex.builder()
                        .key(legalFact.getKey())
                        .category(legalFact.getCategory() != null ? LegalFactCategoryInt.valueOf(legalFact.getCategory().getValue()) : null)
                        .recIndex(legalFact.getRecIndex())
                        .build()
                )
                .toList();
    }
        
}
