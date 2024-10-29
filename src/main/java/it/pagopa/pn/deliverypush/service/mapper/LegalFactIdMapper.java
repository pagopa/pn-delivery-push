package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategoryV20;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsIdV20;

public class LegalFactIdMapper {
    private LegalFactIdMapper(){}
    
    public static LegalFactsIdV20 internalToExternal(LegalFactsIdInt dtoInt){
        return LegalFactsIdV20.builder()
                .key(dtoInt.getKey())
                .category( dtoInt.getCategory() != null ? LegalFactCategoryV20.fromValue(dtoInt.getCategory().getValue()): null)
                .build();
    }
        
}
