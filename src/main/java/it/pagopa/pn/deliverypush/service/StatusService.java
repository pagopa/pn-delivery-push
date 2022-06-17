package it.pagopa.pn.deliverypush.service;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

public interface StatusService {

    @Data
    @AllArgsConstructor
    class NotificationStatusUpdate{
        private NotificationStatusInt oldStatus;
        private NotificationStatusInt newStatus;
    }
    /**
     * controlla e aggiorna lo stato
     * @param dto dto da inserire
     * @param currentTimeline set corrente
     * @param notification notifica
     * @return entrambi i notificationstatus (old, new)
     */
    NotificationStatusUpdate checkAndUpdateStatus(TimelineElementInternal dto, Set<TimelineElementInternal> currentTimeline, NotificationInt notification);
}
