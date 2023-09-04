package it.pagopa.pn.deliverypush.action.cancellation;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;

@Component
@AllArgsConstructor
@CustomLog
public class NotificationCancellationActionHandler {

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;

    public void cancelNotification(String iun){
        log.debug("Start cancelNotification - iun={}", iun);

        // chiedo la cancellazione degli IUV
        notificationService.removeAllNotificationCostsByIun(iun).block();

        NotificationInt notification = notificationService.getNotificationByIun(iun);

        // salvo l'evento in timeline
        TimelineElementInternal cancelledTimelineElement = addCanceledTimelineElement(iun, notification);

        // avviso delivery del cambio di stato
        notificationService.updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, cancelledTimelineElement.getTimestamp()).block();

    }

    private TimelineElementInternal addCanceledTimelineElement(String iun, NotificationInt notification) {
        TimelineElementInternal cancelledTimelineElement = timelineUtils.buildCancelledTimelineElement(notification);

        // salvo l'evento in timeline
        boolean insertSkipped = timelineService.addTimelineElement(cancelledTimelineElement, notification);

        if (insertSkipped)
        {
            // devo recuperarmi il vero timeline event per sapere il suo timestamp, evidentemente c'è stato un errore nell'update a delivery
            Optional<TimelineElementInternal> timelineElementInternal = timelineService.getTimelineElement(iun, cancelledTimelineElement.getElementId());
            if (timelineElementInternal.isPresent())
            {
                cancelledTimelineElement = timelineElementInternal.get();
            }
            else
            {
                throw new PnInternalException("timeline element not found but insert was skipped elementid=" + cancelledTimelineElement.getElementId(), ERROR_CODE_PN_GENERIC_ERROR);
            }
        }
        return cancelledTimelineElement;
    }

}
