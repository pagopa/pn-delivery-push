package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

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
    public Integer getNotificationCost(NotificationInt notification, Integer recIndex) {
        Integer notificationCost = null;

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
            notificationCost = notificationProcessCostService.getNotificationProcessCost(notification.getIun(), recIndex).block();
        }
        return notificationCost;
    }

}
