package it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.DocumentCreationRequestDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.DocumentCreationRequestEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.entity.DocumentCreationRequestEntity;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationdao.dynamo.mapper.DtoToEntityDocumentCreationRequestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = DocumentCreationRequestDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class DocumentCreationRequestDaoDynamo implements DocumentCreationRequestDao {

    private final DocumentCreationRequestEntityDao dao;
    private final DtoToEntityDocumentCreationRequestMapper dtoToEntity;

    public DocumentCreationRequestDaoDynamo(DocumentCreationRequestEntityDao dao,
                                            DtoToEntityDocumentCreationRequestMapper dtoToEntity) {
        this.dao = dao;
        this.dtoToEntity = dtoToEntity;
    }
    
    @Override
    public void addDocumentCreationRequest(DocumentCreationRequest documentCreationRequest) {
        DocumentCreationRequestEntity entity = dtoToEntity.dto2Entity(documentCreationRequest);
        dao.put(entity);
    }

}

