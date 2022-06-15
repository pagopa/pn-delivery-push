package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;

public class LegalFactIdMapper {
    private LegalFactIdMapper(){}
    
    public static LegalFactsId internalToExternal(LegalFactsIdInt dtoInt){
        return LegalFactsId.builder()
                .key(dtoInt.getKey())
                .category( dtoInt.getCategory() != null ? LegalFactCategory.fromValue(dtoInt.getCategory().getValue()): null)
                .build();
    }
        
}
