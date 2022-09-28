package it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo.mapper;

import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
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
