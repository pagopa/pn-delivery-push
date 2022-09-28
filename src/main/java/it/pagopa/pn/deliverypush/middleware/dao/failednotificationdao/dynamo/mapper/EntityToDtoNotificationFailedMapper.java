package it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoNotificationFailedMapper {

    public PaperNotificationFailed entityToDto(PaperNotificationFailedEntity entity) {
        return PaperNotificationFailed.builder()
                .iun(entity.getIun())
                .recipientId(entity.getRecipientId())
                .build();
    }
}

