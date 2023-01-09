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
    public Mono<Integer> getNotificationCost(NotificationInt notification, Integer recIndex) {
        //Trasformato in MONO anche per sviluppi futuri, in modo da adeguare correttamente i client
        Mono<Integer> notificationCost = Mono.empty();

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
            notificationCost = notificationCostService.getNotificationCost(notification, recIndex);
        }
        return notificationCost;
    }

}
