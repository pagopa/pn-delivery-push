package it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.entity.DocumentCreationRequestEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoDocumentCreationRequestMapper {

    public DocumentCreationRequest entityToDto(DocumentCreationRequestEntity entity) {
        return DocumentCreationRequest.builder()
                .key(entity.getKey())
                .iun(entity.getIun())
                .recIndex(entity.getRecIndex())
                .documentCreationType(DocumentCreationTypeInt.valueOf(entity.getDocumentType()))
                .timelineId(entity.getTimelineId())
                .build();
    }
}

