package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategoryV28;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsIdV28;

public class LegalFactIdMapper {
    private LegalFactIdMapper(){}
    
    public static LegalFactsIdV28 internalToExternal(LegalFactsIdInt dtoInt){
        return LegalFactsIdV28.builder()
                .key(dtoInt.getKey())
                .category( dtoInt.getCategory() != null ? LegalFactCategoryV28.fromValue(dtoInt.getCategory().getValue()): null)
                .build();
    }
        
}
