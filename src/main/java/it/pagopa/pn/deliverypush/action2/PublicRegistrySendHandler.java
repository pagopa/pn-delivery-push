package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.external.PublicRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PublicRegistrySendHandler {
    private final PublicRegistryUtils publicRegistryUtils;
    private final PublicRegistry publicRegistry;
    private final NotificationUtils notificationUtils;
    
    public PublicRegistrySendHandler(PublicRegistryUtils publicRegistryUtils, PublicRegistry publicRegistry, NotificationUtils notificationUtils) {
        this.publicRegistryUtils = publicRegistryUtils;
        this.publicRegistry = publicRegistry;
        this.notificationUtils = notificationUtils;
    }

    /**
     * Send get request to public registry for get digital address
     **/
    public void sendRequestForGetDigitalGeneralAddress(Notification notification, int recIndex, ContactPhase contactPhase, int sentAttemptMade) {

        String correlationId = publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, contactPhase, sentAttemptMade, DeliveryMode.DIGITAL);
        log.info("SendRequestForGetDigitalAddress correlationId {} - iun {} id {}", correlationId, notification.getIun(), recIndex);

        publicRegistryUtils.addPublicRegistryCallToTimeline(notification.getIun(), recIndex, contactPhase, sentAttemptMade, correlationId, DeliveryMode.DIGITAL);

        NotificationRecipient recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        publicRegistry.sendRequestForGetDigitalAddress(recipient.getTaxId(), correlationId);

        log.debug("End sendRequestForGetAddress correlationId {} - iun {} id {}", correlationId, notification.getIun(), recIndex);
    }

    /**
     * Send get request to public registry for physical address
     **/
    public void sendRequestForGetPhysicalAddress(Notification notification, int recIndex, int sentAttemptMade) {
        String correlationId = publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, ContactPhase.SEND_ATTEMPT, sentAttemptMade, DeliveryMode.ANALOG);
        log.info("SendRequestForGetPhysicalAddress correlationId {} - iun {} id {}", correlationId, notification.getIun(), recIndex);

        publicRegistryUtils.addPublicRegistryCallToTimeline(notification.getIun(), recIndex, ContactPhase.SEND_ATTEMPT, sentAttemptMade, correlationId, DeliveryMode.ANALOG);
        
        NotificationRecipient recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        publicRegistry.sendRequestForGetPhysicalAddress(recipient.getTaxId(), correlationId);

        log.debug("End sendRequestForGetPhysicalAddress correlationId {} - iun {} id {}", correlationId, notification.getIun(), recIndex);
    }
}
