package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogFailureWorkflowTimeoutDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogWorfklowRecipientDeceasedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RefinementDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class NotificationCost {
    private final NotificationProcessCostService notificationProcessCostService;
    private final TimelineService timelineService;

    public NotificationCost(NotificationProcessCostService notificationProcessCostService,
                            TimelineService timelineService) {
        this.notificationProcessCostService = notificationProcessCostService;
        this.timelineService = timelineService;
    }

    @Nullable
    public Mono<Optional<Integer>> getNotificationCostForViewed(NotificationInt notification, Integer recIndex) {
        record EventChecker(String elementId, Function<TimelineElementInternal, Boolean> costChecker) {}

        List<EventChecker> checkers = List.of(
                new EventChecker(
                        getRefinementId(notification.getIun(), recIndex),
                        NotificationCost::refinementHasCost
                ),
                new EventChecker(
                        getDeceasedId(notification.getIun(), recIndex),
                        NotificationCost::deceasedEventHasCost
                ),
                new EventChecker(
                        getAnalogFailureWorkflowTimeoutId(notification.getIun(), recIndex),
                        NotificationCost::analogFailureWorkflowTimeoutHasCost
                )
        );

        return Mono.defer(() -> {
            for (EventChecker checker : checkers) {
                String eventId = checker.elementId;
                Optional<TimelineElementInternal> elementOpt = timelineService.getTimelineElementStrongly(notification.getIun(), eventId);
                if (elementOpt.isPresent() && checker.costChecker().apply(elementOpt.get())) {
                    log.debug("Element {} with cost found for notification {} recIndex {}", eventId, notification.getIun(), recIndex);
                    return Mono.just(Optional.empty());
                }
                log.debug("Element {} with cost not found for notification {} recIndex {}", eventId, notification.getIun(), recIndex);
            }

            log.debug("No elements with cost found for notification {} recIndex {}, returning send fee", notification.getIun(), recIndex);
            return notificationProcessCostService.getSendFeeAsync().map(Optional::of);
        });
    }

    private String getRefinementId(String iun, Integer recIndex) {
        // Costruisce l'ID dell'evento di refinement
        return TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
    }

    private static boolean refinementHasCost(TimelineElementInternal timelineElement) {
        return ((RefinementDetailsInt)timelineElement.getDetails()).getNotificationCost() != null
                && ((RefinementDetailsInt)timelineElement.getDetails()).getNotificationCost() != 0;
    }

    private static boolean deceasedEventHasCost(TimelineElementInternal timelineElement) {
        return ((AnalogWorfklowRecipientDeceasedDetailsInt) timelineElement.getDetails()).getNotificationCost() != null
                && ((AnalogWorfklowRecipientDeceasedDetailsInt) timelineElement.getDetails()).getNotificationCost() != 0;
    }

    private String getDeceasedId(String iun, Integer recIndex) {
        // Costruisce l'ID dell'evento di deceased
        return TimelineEventId.ANALOG_WORKFLOW_RECIPIENT_DECEASED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
    }

    private String getAnalogFailureWorkflowTimeoutId(String iun, Integer recIndex) {
        // Costruisce l'ID dell'evento di ANALOG_FAILURE_WORKFLOW_TIMEOUT
        return TimelineEventId.ANALOG_FAILURE_WORKFLOW_TIMEOUT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
    }

    private static boolean analogFailureWorkflowTimeoutHasCost(TimelineElementInternal timelineElement) {
        return ((AnalogFailureWorkflowTimeoutDetailsInt) timelineElement.getDetails()).getNotificationCost() != null &&
                ((AnalogFailureWorkflowTimeoutDetailsInt) timelineElement.getDetails()).getNotificationCost() != 0;
    }

}
