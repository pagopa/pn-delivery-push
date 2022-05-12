package it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
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

