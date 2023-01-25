package it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo.entity.DocumentCreationRequestEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoDocumentCreationRequestMapper {

    public DocumentCreationRequest entityToDto(DocumentCreationRequestEntity entity) {
        return DocumentCreationRequest.builder()
                .key(entity.getKey())
                .iun(entity.getIun())
                .recIndex(entity.getRecIndex())
                .documentCreationType(DocumentCreationRequest.DocumentCreationType.valueOf(entity.getDocumentType()))
                .build();
    }
}

