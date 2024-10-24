package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV25;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamRequestV25;
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

    public static StreamEntity dtoToEntity(String paId, String streamId, String version, StreamCreationRequestV25 dto) {
        StreamEntity streamEntity = new StreamEntity(paId, streamId);
        streamEntity.setEventType(dto.getEventType().getValue());
        streamEntity.setTitle(dto.getTitle());
        streamEntity.setVersion(version != null ? version : currentVersion);
        streamEntity.setGroups(dto.getGroups());
        if (dto.getFilterValues() != null && !dto.getFilterValues().isEmpty())
            streamEntity.setFilterValues(Set.copyOf(dto.getFilterValues()));
        else
            streamEntity.setFilterValues(null);
        return streamEntity;
    }

    public static StreamEntity dtoToEntity(String paId, String streamId, String version, StreamRequestV25 dto) {
        StreamCreationRequestV25 creationRequestv23 = new StreamCreationRequestV25();
        BeanUtils.copyProperties(dto, creationRequestv23);
        creationRequestv23.setEventType(StreamCreationRequestV25.EventTypeEnum.fromValue(dto.getEventType().getValue()));
        return dtoToEntity(paId, streamId, version, creationRequestv23);
    }
}
