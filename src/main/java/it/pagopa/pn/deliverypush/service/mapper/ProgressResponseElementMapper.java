package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.ProgressResponseElementV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.RefusedReason;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.TimelineElementCategoryV23;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.RefusedReasonEntity;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.mapper.EntityToDtoRefusedReasonMapper;
import java.util.List;
import org.springframework.util.CollectionUtils;

public class ProgressResponseElementMapper {
    private ProgressResponseElementMapper() {
    }

    public static ProgressResponseElementV23 internalToExternalv23(EventEntity ev) {
        ProgressResponseElementV23 progressResponseElement = new ProgressResponseElementV23();
        progressResponseElement.setEventId(ev.getEventId());
        progressResponseElement.setTimestamp(ev.getTimestamp());
        progressResponseElement.setIun(ev.getIun());
        progressResponseElement.setNewStatus(ev.getNewStatus() != null ? NotificationStatus.valueOf(ev.getNewStatus()) : null);
        progressResponseElement.setNotificationRequestId(ev.getNotificationRequestId());
        progressResponseElement.setTimelineEventCategory(TimelineElementCategoryV23.fromValue(ev.getTimelineEventCategory()));
        progressResponseElement.setChannel(ev.getChannel());
        progressResponseElement.setRecipientIndex(ev.getRecipientIndex());
        progressResponseElement.setLegalfactIds(ev.getLegalfactIds());
        progressResponseElement.setAnalogCost(ev.getAnalogCost());
        progressResponseElement.setValidationErrors( !CollectionUtils.isEmpty( ev.getValidationErrors() ) ? mapRefusedReasons( ev.getValidationErrors() ) : null );
        return progressResponseElement;
    }
    private static List<RefusedReason> mapRefusedReasons(List<RefusedReasonEntity> refusedReasonEntityList) {
        return refusedReasonEntityList.stream()
                .map(EntityToDtoRefusedReasonMapper::entityToDto)
                .toList();
    }

}
