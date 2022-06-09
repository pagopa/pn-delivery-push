package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponse;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class EntityToDtoStreamMapper {

    private EntityToDtoStreamMapper(){}

    public static StreamMetadataResponse entityToDto(StreamEntity entity ) {
        StreamMetadataResponse streamMetadataResponse = new StreamMetadataResponse();
        streamMetadataResponse.setStreamId(UUID.fromString(entity.getStreamId()));
        streamMetadataResponse.setActivationDate(entity.getActivationDate());
        streamMetadataResponse.setEventType(StreamMetadataResponse.EventTypeEnum.valueOf(entity.getEventType()));
        streamMetadataResponse.setTitle(entity.getTitle());
        streamMetadataResponse.setFilterValues(List.copyOf(Objects.requireNonNullElse(entity.getFilterValues(), new HashSet<>())));
        return streamMetadataResponse;
    }

}