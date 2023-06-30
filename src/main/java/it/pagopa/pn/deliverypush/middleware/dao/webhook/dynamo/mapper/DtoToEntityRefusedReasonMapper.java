package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.RefusedReason;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.RefusedReasonEntity;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityRefusedReasonMapper {

    private DtoToEntityRefusedReasonMapper(){}

    public static RefusedReasonEntity dtoToEntity(RefusedReason refusedReason) {
        RefusedReasonEntity refusedReasonEntity = new RefusedReasonEntity();
        refusedReasonEntity.setErrorCode( refusedReason.getErrorCode() );
        refusedReasonEntity.setDetail( refusedReason.getDetail() );
        return refusedReasonEntity;
    }
}
