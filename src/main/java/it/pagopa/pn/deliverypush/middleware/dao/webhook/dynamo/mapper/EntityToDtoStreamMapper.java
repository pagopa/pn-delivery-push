package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.deliverypush.utils.Constants;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoStreamMapper {

    private EntityToDtoStreamMapper(){}

    public static StreamMetadataResponseV23 entityToDto(StreamEntity entity ) {
        StreamMetadataResponseV23 streamMetadataResponse = new StreamMetadataResponseV23();
        streamMetadataResponse.setStreamId(UUID.fromString(entity.getStreamId()));
        streamMetadataResponse.setActivationDate(entity.getActivationDate());
        streamMetadataResponse.setEventType(StreamMetadataResponseV23.EventTypeEnum.valueOf(entity.getEventType()));
        streamMetadataResponse.setTitle(entity.getTitle());
        streamMetadataResponse.setFilterValues(List.copyOf(Objects.requireNonNullElse(entity.getFilterValues(), new HashSet<>())));
        streamMetadataResponse.setGroups(entity.getGroups());
        streamMetadataResponse.setVersion(entity.getVersion() != null ? entity.getVersion() : Constants.API_FIRST_VERSION);
        return streamMetadataResponse;
    }

}