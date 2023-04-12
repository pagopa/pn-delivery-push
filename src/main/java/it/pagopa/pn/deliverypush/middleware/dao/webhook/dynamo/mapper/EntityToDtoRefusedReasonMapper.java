package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.RefusedReason;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.RefusedReasonEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoRefusedReasonMapper {

    private EntityToDtoRefusedReasonMapper(){}

    public static RefusedReason entityToDto( RefusedReasonEntity entity) {
        RefusedReason refusedReason = new RefusedReason();
        refusedReason.setErrorCode( entity.getErrorCode() );
        refusedReason.setDetail( entity.getDetail() );
        return refusedReason;
    }

}
