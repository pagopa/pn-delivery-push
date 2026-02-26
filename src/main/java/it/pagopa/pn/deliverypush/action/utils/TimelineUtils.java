package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId.*;


@Component
@Slf4j
public class TimelineUtils {

    private final TimelineService timelineService;

    public TimelineUtils(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    public TimelineElementInternal buildTimeline(NotificationInt notification,
                                                 TimelineElementCategoryInt category,
                                                 String elementId,
                                                 @NotNull TimelineElementDetailsInt details) {

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, category, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildTimeline(NotificationInt notification,
                                                 TimelineElementCategoryInt category,
                                                 String elementId,
                                                 TimelineElementDetailsInt details,
                                                 TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder) {
        return timelineBuilder
                .iun(notification.getIun())
                .category(category)
                .timestamp(Instant.now())
                .elementId(elementId)
                .details(details)
                .paId(notification.getSender().getPaId())
                .notificationSentAt(notification.getSentAt())
                .build();
    }

    public TimelineElementInternal buildNotificationViewedLegalFactCreationRequestTimelineElement(
            NotificationInt notification,
            String legalFactId,
            NotificationViewedInt notificationViewed
    ) {
        log.debug("buildNotificationViewedLegalFactCreationRequestTimelineElement - iun={} and id={}", notification.getIun(), notificationViewed.getRecipientIndex());

        String elementId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(notificationViewed.getRecipientIndex())
                        .build());

        NotificationViewedCreationRequestDetailsInt details = NotificationViewedCreationRequestDetailsInt.builder()
                .recIndex(notificationViewed.getRecipientIndex())
                .legalFactId(legalFactId)
                .raddType(notificationViewed.getRaddInfo() != null ? notificationViewed.getRaddInfo().getType() : null)
                .raddTransactionId(notificationViewed.getRaddInfo() != null ? notificationViewed.getRaddInfo().getTransactionId() : null)
                .delegateInfo(notificationViewed.getDelegateInfo())
                .eventTimestamp(notificationViewed.getViewedDate())
                .sourceChannel(notificationViewed.getSourceChannel())
                .sourceChannelDetails(notificationViewed.getSourceChannelDetails())
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());

        return buildTimeline(notification, TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST, elementId,
                details, timelineBuilder);
    }


    public TimelineElementInternal buildCancelRequestTimelineElement(NotificationInt notification) {
        log.debug("buildCancelRequestTimelineElement - IUN={}", notification.getIun());

        String elementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());
        NotificationCancellationRequestDetailsInt details = NotificationCancellationRequestDetailsInt.builder().
                cancellationRequestId(UUID.randomUUID().toString()).
                build();
        return buildTimeline(notification, TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST, elementId, details);
    }

    public boolean checkIsNotificationViewed(String iun, Integer recIndex) {
        log.debug("checkNotificationIsAlreadyViewed - iun={} recIndex={}", iun, recIndex);

        Optional<TimelineElementInternal> notificationViewCreationRequestOpt = getNotificationViewCreationRequest(iun, recIndex);

        if (notificationViewCreationRequestOpt.isEmpty()) {
            log.debug("notificationViewCreationRequest is not present - iun={} recIndex={}", iun, recIndex);

            Optional<TimelineElementInternal> notificationViewOpt = getNotificationView(iun, recIndex);
            log.debug("notificationViewOpt is={} - iun={} recIndex={}", notificationViewOpt.isPresent(), iun, recIndex);

            return notificationViewOpt.isPresent();
        }

        return true;
    }


    public boolean checkIsNotificationRefined(String iun, Integer recIndex) {
        log.debug("checkIsNotificationRefined - iun={} recIndex={}", iun, recIndex);
        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<TimelineElementInternal> notificationRefinedRequestOpt = timelineService.getTimelineElement(iun, elementId);

        log.debug("check notification refined is {} - iun={} recIndex={}", notificationRefinedRequestOpt.isPresent(), iun, recIndex);
        return notificationRefinedRequestOpt.isPresent();
    }

    public boolean checkIsRecipientDeceased(String iun, Integer recIndex) {
        log.debug("checkIsRecipientDeceased - iun={} recIndex={}", iun, recIndex);
        Optional<TimelineElementInternal> elementInternalOptional = getNotificationRecipientDeceased(iun, recIndex);

        log.debug("check recipient deceased is {} - iun={} recIndex={}", elementInternalOptional.isPresent(), iun, recIndex);
        return elementInternalOptional.isPresent();
    }

    private Optional<TimelineElementInternal> getNotificationRecipientDeceased(String iun, Integer recIndex) {
        String elementId = TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        return timelineService.getTimelineElement(iun, elementId);
    }

    public boolean checkIsNotificationCancellationRequested(String iun) {
        String elementId = NOTIFICATION_CANCELLATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        Optional<TimelineElementInternal> notificationElement = timelineService.getTimelineElement(iun, elementId);

        boolean isNotificationCancelled = notificationElement.isPresent();
        log.debug("NotificationCancelled value is={}", isNotificationCancelled);

        return isNotificationCancelled;
    }

    public boolean checkIsNotificationCancelledLegalFactId(String iun, String legalFactId) {
        log.debug("Start checkIsNotificationCancelledLegalFactId - iun={} legalFactId={}", iun, legalFactId);
        
        String elementId = NOTIFICATION_CANCELLED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .build());

        Optional<TimelineElementInternal> notificationCancelledOpt = timelineService.getTimelineElement(iun, elementId);
        
        if(notificationCancelledOpt.isPresent()){
            TimelineElementInternal notificationCancelled = notificationCancelledOpt.get();
            return notificationCancelled.getLegalFactsIds().stream().anyMatch(legalFactsIdInt -> {
                String legalFactKeyReplaced = legalFactsIdInt.getKey().replace(PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX, "");
                return legalFactKeyReplaced.equals(legalFactId);
            });
        }
        
        return false;
    }

    public TimelineElementInternal buildNotificationRaddRetrieveTimelineElement(
            NotificationInt notification,
            Integer recIndex,
            RaddInfo raddInfo,
            Instant eventTimestamp) {
        log.debug("buildNotificationRaddRetrieveTimelineElement - iun={} and transactionId={}", notification.getIun(), raddInfo.getTransactionId());

        String elementId = TimelineEventId.NOTIFICATION_RADD_RETRIEVED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        NotificationRADDRetrievedDetailsInt details = NotificationRADDRetrievedDetailsInt.builder()
                .recIndex(recIndex)
                .eventTimestamp(eventTimestamp)
                .raddType(raddInfo.getType())
                .raddTransactionId(raddInfo.getTransactionId())
                .build();


        return buildTimeline(notification, TimelineElementCategoryInt.NOTIFICATION_RADD_RETRIEVED, elementId, details);
    }


    private Optional<TimelineElementInternal> getNotificationView(String iun, Integer recIndex) {
        String elementId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        return timelineService.getTimelineElement(iun, elementId);
    }

    public Optional<TimelineElementInternal> getNotificationViewCreationRequest(String iun, Integer recIndex) {
        String elementId = TimelineEventId.NOTIFICATION_VIEWED_CREATION_REQUEST.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        return timelineService.getTimelineElement(iun, elementId);
    }



}
