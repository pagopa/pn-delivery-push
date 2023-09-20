package it.pagopa.pn.deliverypush.action.cancellation;

import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@CustomLog
public class NotificationCancellationActionHandler {

    private final NotificationCancellationService notificationCancellationService;

    public void cancelNotification(String iun){
        log.debug("Start cancelNotification - iun={}", iun);

        notificationCancellationService.completeCancellationProcess(iun);
    }

}
