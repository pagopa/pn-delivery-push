package it.pagopa.pn.deliverypush.action.startworkflowrecipient;

import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class StartWorkflowForRecipientHandler {
    private final AarUtils aarUtils;
    private final NotificationService notificationService;
    
    public void startNotificationWorkflowForRecipient(String iun, int recIndex, RecipientsWorkflowDetails details) {
        log.info("Start notification workflow for recipient - iun {} id {} token {}", iun, recIndex, details.getQuickAccessLinkToken());
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        // ... genero il pdf dell'AAR, salvo su Safestorage e genero elemento in timeline AAR_GENERATION, potrebbe servirmi dopo ...
        aarUtils.generateAARAndSaveInSafeStorageAndAddTimelineEvent(notification, recIndex, details.getQuickAccessLinkToken());
    }
    
}
 