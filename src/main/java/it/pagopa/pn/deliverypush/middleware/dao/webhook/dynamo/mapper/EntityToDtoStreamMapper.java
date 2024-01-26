package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponsev23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoStreamMapper {

    private EntityToDtoStreamMapper(){}

    public static StreamMetadataResponsev23 entityToDto(StreamEntity entity ) {
        StreamMetadataResponsev23 streamMetadataResponse = new StreamMetadataResponsev23();
        streamMetadataResponse.setStreamId(UUID.fromString(entity.getStreamId()));
        streamMetadataResponse.setActivationDate(entity.getActivationDate());
        streamMetadataResponse.setEventType(StreamMetadataResponsev23.EventTypeEnum.valueOf(entity.getEventType()));
        streamMetadataResponse.setTitle(entity.getTitle());
        streamMetadataResponse.setFilterValues(List.copyOf(Objects.requireNonNullElse(entity.getFilterValues(), new HashSet<>())));
        return streamMetadataResponse;
    }

}