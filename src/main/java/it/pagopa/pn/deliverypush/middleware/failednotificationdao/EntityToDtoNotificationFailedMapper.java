package it.pagopa.pn.deliverypush.middleware.failednotificationdao;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.middleware.model.entity.PaperNotificationFailedEntity;
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

