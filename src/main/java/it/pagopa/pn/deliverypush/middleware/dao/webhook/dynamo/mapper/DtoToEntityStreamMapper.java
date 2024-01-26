package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequest;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestv23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamUpdateRequestv23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityStreamMapper {

    private DtoToEntityStreamMapper(){}

    //IVAN: con fabrizio: questa non serve più immagino
    public static StreamEntity dtoToEntity(String paId, String streamId, StreamCreationRequest dto) {
        StreamEntity streamEntity = new StreamEntity(paId, streamId);
        streamEntity.setEventType(dto.getEventType().getValue());
        streamEntity.setTitle(dto.getTitle());
        if (dto.getFilterValues() != null && !dto.getFilterValues().isEmpty())
            streamEntity.setFilterValues(Set.copyOf(dto.getFilterValues()));
        else
            streamEntity.setFilterValues(null);
        return streamEntity;
    }

    public static StreamEntity dtoToEntity(String paId, String streamId, StreamCreationRequestv23 dto) {
        StreamEntity streamEntity = new StreamEntity(paId, streamId);
        streamEntity.setEventType(dto.getEventType().getValue());
        streamEntity.setTitle(dto.getTitle());
        if (dto.getFilterValues() != null && !dto.getFilterValues().isEmpty())
            streamEntity.setFilterValues(Set.copyOf(dto.getFilterValues()));
        else
            streamEntity.setFilterValues(null);
        return streamEntity;
    }

    //IVAN: capire se mantenere StreamUpdateRequestv23
    public static StreamEntity dtoToEntity(String paId, String streamId, StreamUpdateRequestv23 dto) {
        StreamCreationRequestv23 creationRequestv23 = new StreamCreationRequestv23();
        BeanUtils.copyProperties(dto, creationRequestv23);
        return dtoToEntity(paId, streamId, creationRequestv23);
    }
}
