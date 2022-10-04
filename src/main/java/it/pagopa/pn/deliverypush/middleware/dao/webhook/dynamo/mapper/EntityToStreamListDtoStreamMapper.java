package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EntityToStreamListDtoStreamMapper {
    
    private EntityToStreamListDtoStreamMapper(){
        
    }

    public static StreamListElement entityToDto(StreamEntity entity ) {
        StreamListElement streamListElement = new StreamListElement();
        streamListElement.setStreamId(UUID.fromString(entity.getStreamId()));
        streamListElement.setTitle(entity.getTitle());
        return streamListElement;
    }

}