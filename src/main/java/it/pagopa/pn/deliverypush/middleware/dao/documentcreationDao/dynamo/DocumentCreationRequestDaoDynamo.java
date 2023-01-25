package it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.DocumentCreationRequestDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.DocumentCreationRequestEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo.entity.DocumentCreationRequestEntity;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo.mapper.DtoToEntityDocumentCreationRequestMapper;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo.mapper.EntityToDtoDocumentCreationRequestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = DocumentCreationRequestDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class DocumentCreationRequestDaoDynamo implements DocumentCreationRequestDao {

    private final DocumentCreationRequestEntityDao dao;
    private final DtoToEntityDocumentCreationRequestMapper dtoToEntity;
    private final EntityToDtoDocumentCreationRequestMapper entityToDto;

    public DocumentCreationRequestDaoDynamo(DocumentCreationRequestEntityDao dao,
                                            DtoToEntityDocumentCreationRequestMapper dtoToEntity, EntityToDtoDocumentCreationRequestMapper entityToDto) {
        this.dao = dao;
        this.dtoToEntity = dtoToEntity;
        this.entityToDto = entityToDto;
    }
    
    @Override
    public void addDocumentCreationRequest(DocumentCreationRequest documentCreationRequest) {
        DocumentCreationRequestEntity entity = dtoToEntity.dto2Entity(documentCreationRequest);
        dao.put(entity);
    }

    @Override
    public Optional<DocumentCreationRequest>  getDocumentCreationRequest(String fileKey) {
        Key keyToSearch = Key.builder()
                .partitionValue(fileKey)
                .build();

        return dao.get(keyToSearch)
                .map(entityToDto::entityToDto);
    }
}

