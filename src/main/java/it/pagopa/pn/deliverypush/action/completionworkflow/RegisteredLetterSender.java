package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RegisteredLetterSender {
    private final NotificationUtils notificationUtils;
    private final ExternalChannelService externalChannelService;

    public RegisteredLetterSender(NotificationUtils notificationUtils,
                                  ExternalChannelService externalChannelService) {
        this.notificationUtils = notificationUtils;
        this.externalChannelService = externalChannelService;
    }

    /**
     * Sent notification by simple registered letter
     */
    public void sendSimpleRegisteredLetter(NotificationInt notification, Integer recIndex) {
        log.info("Start send simple registered letter  - iun {} id {}", notification.getIun(), recIndex);
        //Al termine del workflow digitale se non si è riusciti ad contattare in nessun modo il recipient, viene inviata una raccomanda semplice

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        PhysicalAddressInt physicalAddress = recipient.getPhysicalAddress();

        // NOTA è previsto che il physicalAddress sia obbligatorio anche fuori MVP altrimenti la notifica non passa mai in delivered PN-2509
        if (physicalAddress != null) {
            log.info("Sending simple registered letter  - iun {} id {}", notification.getIun(), recIndex);
            externalChannelService.sendNotificationForRegisteredLetter(notification, physicalAddress, recIndex);
        } else {
            log.error("Simple registered letter can't be send, there isn't physical address for recipient. iun {} id {}", notification.getIun(), recIndex);
        }
    }
}
