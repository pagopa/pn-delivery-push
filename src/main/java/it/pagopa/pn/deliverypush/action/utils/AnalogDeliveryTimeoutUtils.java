package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogTimeoutCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED;

@Component
@AllArgsConstructor
@Slf4j
public class AnalogDeliveryTimeoutUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final AarUtils aarUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfig;
    private final AttachmentUtils attachmentUtils;
    private final NotificationProcessCostService notificationProcessCostService;

    public boolean isSendAnalogTimeoutCreationRequestPresent(String iun, int recIndex, Integer sentAttemptMade) {
        log.info("isSendAnalogTimeoutCreationRequestPresent with iun={} - recipient index={} - sentAttemptMade={}", iun, recIndex, sentAttemptMade);
        Optional<SendAnalogTimeoutCreationRequestDetailsInt> timelineElementInternalOpt = getSendAnalogTimeoutCreationRequestDetails(iun, recIndex, sentAttemptMade);
        return timelineElementInternalOpt.isPresent();
    }

    public Optional<SendAnalogTimeoutCreationRequestDetailsInt> getSendAnalogTimeoutCreationRequestDetails(String iun, int recIndex, Integer sentAttemptMade) {
        log.info("getSendAnalogTimeoutCreationRequestDetails with iun={} - recipient index={} - sentAttemptMade={}", iun, recIndex, sentAttemptMade);
        String timelineId = TimelineEventId.SEND_ANALOG_TIMEOUT_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
        return timelineService.getTimelineElementDetails(iun, timelineId, SendAnalogTimeoutCreationRequestDetailsInt.class);
    }

    public void buildAnalogFailureWorkflowTimeoutElement(NotificationInt notification,
                                                         int recIndex,
                                                         Instant timeoutDate){

        Integer retentionAttachmentDaysAfterRefinement = pnDeliveryPushConfig.getRetentionAttachmentDaysAfterDeliveryTimeout();
        boolean addNotificationCost = true;
        AarGenerationDetailsInt aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);
        // Se la notifica è stata precedentemente visualizzata, non si aggiunge il costo della notifica e non si aggiorna la retention dei documenti
        if(isNotificationViewed(notification.getIun(), recIndex)){
            retentionAttachmentDaysAfterRefinement = null;
            addNotificationCost = false;
        }
        try {
            if (retentionAttachmentDaysAfterRefinement != null && retentionAttachmentDaysAfterRefinement != 0) {
                attachmentUtils.changeAttachmentsRetention(notification, retentionAttachmentDaysAfterRefinement).blockLast();
            }

            int notificationCost = notificationProcessCostService.getSendFeeAsync().block();

            TimelineElementInternal analogFailureWorkflowTimeoutElementInternal =
                    timelineUtils.buildAnalogFailureWorkflowTimeout(notification, recIndex, aarGenerationDetails.getGeneratedAarUrl(), notificationCost, timeoutDate, addNotificationCost);

            timelineService.addTimelineElement(analogFailureWorkflowTimeoutElementInternal, notification);
        } catch (Exception ex) {
            throw new PnInternalException("Exception adding analog failure timeout timeline element for notification with iun=" + notification.getIun() + " and recIndex=" + recIndex, ERROR_CODE_DELIVERYPUSH_ADDTIMELINEFAILED);
        }
    }

    private boolean isNotificationViewed(String iun, int recIndex) {
        return timelineUtils.checkIsNotificationViewed(iun, recIndex);
    }

}
