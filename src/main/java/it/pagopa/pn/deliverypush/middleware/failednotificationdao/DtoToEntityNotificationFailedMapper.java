package it.pagopa.pn.deliverypush.middleware.failednotificationdao;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityNotificationFailedMapper {
    
    public PaperNotificationFailedEntity dto2Entity(PaperNotificationFailed dto) {
        return PaperNotificationFailedEntity.builder()
                .recipientId(dto.getRecipientId())
                .iun(dto.getIun())
                .build();
    }
}
