package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityStreamMapper {

    private static String currentVersion;

    public DtoToEntityStreamMapper(PnDeliveryPushConfigs pnDeliveryPushConfigs){
        currentVersion = pnDeliveryPushConfigs.getWebhook().getCurrentVersion();
    }

    public static StreamEntity dtoToEntity(String paId, String streamId, StreamCreationRequestV23 dto) {
        StreamEntity streamEntity = new StreamEntity(paId, streamId);
        streamEntity.setEventType(dto.getEventType().getValue());
        streamEntity.setTitle(dto.getTitle());
        streamEntity.setVersion(currentVersion);
        streamEntity.setGroups(dto.getGroups());
        if (dto.getFilterValues() != null && !dto.getFilterValues().isEmpty())
            streamEntity.setFilterValues(Set.copyOf(dto.getFilterValues()));
        else
            streamEntity.setFilterValues(null);
        return streamEntity;
    }

    public static StreamEntity dtoToEntity(String paId, String streamId, StreamRequestV23 dto) {
        StreamCreationRequestV23 creationRequestv23 = new StreamCreationRequestV23();
        BeanUtils.copyProperties(dto, creationRequestv23);
        return dtoToEntity(paId, streamId, creationRequestv23);
    }
}
