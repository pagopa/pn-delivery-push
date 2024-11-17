package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogWorfklowRecipientDeceasedDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

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
        //Trasformato in MONO anche per sviluppi futuri, in modo da adeguare correttamente i client

        // Ottiene l'elemento di refinement dalla timeline
        String refinementId = getRefinementId(notification.getIun(), recIndex);
        return Mono.fromCallable(() -> timelineService.getTimelineElementStrongly(notification.getIun(), refinementId))
                .flatMap(timelineElementOpt -> {
                    if(timelineElementOpt.isPresent()) {
                        log.debug("Refinement element found for notification {} recIndex {}", notification.getIun(), recIndex);
                        // Se c'è il refinement restitutiamo un costo empty
                        return Mono.just(Optional.empty());
                    } else {
                        log.debug("Refinement element not found for notification {} recIndex {}", notification.getIun(), recIndex);
                        return handleDeceasedElement(notification.getIun(), recIndex);
                    }
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

    private Mono<Optional<Integer>> handleDeceasedElement(String iun, Integer recIndex) {
        String deceasedId = getDeceasedId(iun, recIndex);
        return Mono.fromCallable(() -> timelineService.getTimelineElementStrongly(iun, deceasedId))
                .flatMap(timelineElementOpt -> {
                    if (timelineElementOpt.isPresent() && deceasedEventHasCost(timelineElementOpt.get())) {
                        log.debug("Deceased element with cost found for notification {} recIndex {}", iun, recIndex);
                        // Se c'è il deceased con un costo applicato restitutiamo un costo empty
                        return Mono.just(Optional.empty());
                    } else {
                        log.debug("Deceased element with cost not found for notification {} recIndex {}", iun, recIndex);
                        return notificationProcessCostService.getSendFeeAsync().map(Optional::of);
                    }
                });
    }

    private static boolean deceasedEventHasCost(TimelineElementInternal timelineElement) {
        return ((AnalogWorfklowRecipientDeceasedDetailsInt) timelineElement.getDetails()).getNotificationCost() != null;
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

}
