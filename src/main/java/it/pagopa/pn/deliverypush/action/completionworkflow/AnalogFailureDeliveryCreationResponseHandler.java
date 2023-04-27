package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogFailureWorkflowCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalDeliveryCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

@Component
@AllArgsConstructor
@Slf4j
public class AnalogFailureDeliveryCreationResponseHandler {
    private final CompletelyUnreachableUtils completelyUnreachableUtils;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final RefinementScheduler refinementScheduler;

    public void handleAnalogFailureDeliveryCreationResponse(String iun, int recIndex, DocumentCreationResponseActionDetails actionDetails) {
        log.info("Start handleAnalogFailureDeliveryCreationResponse - iun={} recIndex={} aarKey={}", iun, recIndex, actionDetails.getKey());
        NotificationInt notification = notificationService.getNotificationByIun(iun);





        PnAuditLogEvent logEvent = createAuditLog(notification, recIndex, actionDetails.getKey());
        logEvent.log();
        
        Optional<AnalogFailureWorkflowCreationRequestDetailsInt> analogFailureWorkflowCreationRequestDetailsIntOpt = timelineService.getTimelineElementDetails(iun, actionDetails.getTimelineId(), AnalogFailureWorkflowCreationRequestDetailsInt.class);

        if (analogFailureWorkflowCreationRequestDetailsIntOpt.isPresent()) {
            AnalogFailureWorkflowCreationRequestDetailsInt timelineDetails = analogFailureWorkflowCreationRequestDetailsIntOpt.get();
            // recupero la data di generazione dell'AAR, per poterla inserire nell'atto opponibile
            String aarUrl = retrieveAARUrlFromTimeline(notification.getIun(), recIndex);

            completelyUnreachableUtils.handleCompletelyUnreachable(notification, recIndex, timelineDetails.getLegalFactId(), aarUrl);
            refinementScheduler.scheduleAnalogRefinement(notification, recIndex, timelineDetails.getCompletionWorkflowDate(), timelineDetails.getEndWorkflowStatus());

        } else {
            logEvent.generateFailure("Error in handleAnalogFailureDeliveryCreationResponse for timelineId={} and key={} - iun={} recIndex={}", actionDetails.getTimelineId(), actionDetails.getKey(), notification.getIun(), recIndex).log();
        }
    }

    private PnAuditLogEvent createAuditLog(NotificationInt notification, int recIndex, String legalFactId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "Saving legalFact type={} fileKey={} - iun={} recIndex={}", LegalFactCategoryInt.ANALOG_FAILURE_DELIVERY, legalFactId, notification.getIun(), recIndex)
                .iun(notification.getIun())
                .build();
    }


    private String retrieveAARUrlFromTimeline(String iun, int recIndex) {
        log.info("retrieveAARUrlFromTimeline iun={} recIndex={}", iun, recIndex);

        Optional<TimelineElementInternal> timelineEvent = timelineService.getTimeline(iun, false)
                .stream().filter(x -> x.getCategory() == TimelineElementCategoryInt.AAR_GENERATION)
                .filter(x -> {
                    if (x.getDetails() instanceof AarGenerationDetailsInt aarGenerationDetailsInt){
                        return aarGenerationDetailsInt.getRecIndex() == recIndex;
                    }
                    return false;
                })
                .findFirst();

        if (timelineEvent.isPresent()) {
            String aarUrl = ((AarGenerationDetailsInt)timelineEvent.get().getDetails()).getGeneratedAarUrl();
            log.info("retrieveAARUrlFromTimeline iun={} recIndex={} aarGenerationUrl={}", iun, recIndex, aarUrl);
            return aarUrl;
        }
        else
        {
            LogUtils.logAlarm(log,"Cannot retrieve AAR generation for iun={} recIndex={}", iun, recIndex);
            throw new PnInternalException("Cannot retrieve AAR generation for Iun " + iun + " id" + recIndex, ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
        }
    }
}
