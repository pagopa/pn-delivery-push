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

        /*
         * Il costo della notifica viene valorizzato in fase di visualizzazione solo se la notifica non è già perfezionata per decorrenza termini
         * in quel caso il costo della notifica sarà sull'elemento di timeline corrispondente
         */
        if( timelineService.getTimelineElement(notification.getIun(), elementId).isEmpty() ){
            notificationCostOpt = Optional.ofNullable(notificationCostService.getNotificationCost(notification, recIndex).block());
        }
        return Mono.just(notificationCostOpt);
    }

}
