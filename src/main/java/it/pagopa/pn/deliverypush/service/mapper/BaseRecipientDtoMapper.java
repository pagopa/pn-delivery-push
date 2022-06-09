package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.datavault.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.deliverypush.dto.ext.datavault.BaseRecipientDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;

public class BaseRecipientDtoMapper {
    private BaseRecipientDtoMapper(){}
    
    public static BaseRecipientDtoInt externalToInternal(BaseRecipientDto dtoExt){
        return BaseRecipientDtoInt.builder()
                .internalId(dtoExt.getInternalId())
                .recipientType(dtoExt.getRecipientType() != null ? RecipientTypeInt.valueOf(dtoExt.getRecipientType().getValue()) : null)
                .denomination(dtoExt.getDenomination())
                .taxId(dtoExt.getTaxId())
                .build();
    }

}

