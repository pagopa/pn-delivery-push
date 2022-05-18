package it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class PaperNotificationFailedDaoDynamo implements PaperNotificationFailedDao{

    private final PaperNotificationFailedEntityDao dao;
    private final DtoToEntityNotificationFailedMapper dtoToEntity;
    private final EntityToDtoNotificationFailedMapper entityToDto;

    public PaperNotificationFailedDaoDynamo(PaperNotificationFailedEntityDao dao,
                                            DtoToEntityNotificationFailedMapper dtoToEntity, EntityToDtoNotificationFailedMapper entityToDto) {
        this.dao = dao;
        this.dtoToEntity = dtoToEntity;
        this.entityToDto = entityToDto;
    }

    @Override
    public void addPaperNotificationFailed(PaperNotificationFailed paperNotificationFailed) {
        PaperNotificationFailedEntity entity = dtoToEntity.dto2Entity(paperNotificationFailed);
        dao.put(entity);
    }

    @Override
    public Set<PaperNotificationFailed> getPaperNotificationFailedByRecipientId(String recipientId) {
        return dao.findByRecipientId(recipientId)
                .stream().map(entityToDto::entityToDto)
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteNotificationFailed(String recipientId, String iun) {
        Key key = Key.builder()
                .partitionValue(recipientId)
                .sortValue(iun)
                .build();
        dao.delete(key);
    }
}

