package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.service.NotificationCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
public class NotificationCost {
    private final NotificationCostService notificationCostService;
    private final TimelineService timelineService;

    public NotificationCost(NotificationCostService notificationCostService,
                            TimelineService timelineService) {
        this.notificationCostService = notificationCostService;
        this.timelineService = timelineService;
    }

    @Nullable
    public Mono<Optional<Integer>> getNotificationCost(NotificationInt notification, Integer recIndex) {
        //Trasformato in MONO anche per sviluppi futuri, in modo da adeguare correttamente i client
        Optional<Integer> notificationCostOpt = Optional.empty();

        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        return Mono.fromCallable( () -> timelineService.getTimelineElement(notification.getIun(), elementId))
                .flatMap( timelineElementOpt -> {
                    if(timelineElementOpt.isEmpty()){
                        return notificationCostService.getNotificationCost(notification, recIndex).map(Optional::of);
                    }else {
                        return Mono.just(notificationCostOpt);
                    }
                });
    }

}
