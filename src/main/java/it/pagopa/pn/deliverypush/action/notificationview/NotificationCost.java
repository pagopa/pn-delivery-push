package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
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
    public Mono<Optional<Integer>> getNotificationCost(NotificationInt notification, Integer recIndex) {
        //Trasformato in MONO anche per sviluppi futuri, in modo da adeguare correttamente i client
        Optional<Integer> notificationCostOpt = Optional.empty();

        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        return Mono.fromCallable( () -> timelineService.getTimelineElementStrongly(notification.getIun(), elementId))
                .flatMap( timelineElementOpt -> {
                    if(timelineElementOpt.isEmpty()){
                        return notificationProcessCostService.getPagoPaNotificationBaseCost().map(Optional::of);
                    }else {
                        return Mono.just(notificationCostOpt);
                    }
                });
    }

}
