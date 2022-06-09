package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.mandate.MandateDtoInt;
import it.pagopa.pn.mandate.generated.openapi.clients.mandate.model.InternalMandateDto;

import java.time.Instant;

public class MandateDtoMapper {

    public static MandateDtoInt externalToInternal(InternalMandateDto dtoExt){
        return MandateDtoInt.builder()
                .dateFrom( dtoExt.getDatefrom() != null ? Instant.parse(dtoExt.getDatefrom()) : null )
                .dateTo( dtoExt.getDateto() != null ? Instant.parse(dtoExt.getDateto()) : null )
                .mandateId( dtoExt.getMandateId() )
                .delegate( dtoExt.getDelegate() )
                .delegator( dtoExt.getDelegator() )
                .visibilityIds( dtoExt.getVisibilityIds() )
                .build();
    }
    
}
