package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.externalclient.publicregistry.PublicRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PublicRegistrySendHandler {
    private final PublicRegistryUtils publicRegistryUtils;
    private final PublicRegistry publicRegistry;
    private final NotificationUtils notificationUtils;
    
    public PublicRegistrySendHandler(PublicRegistryUtils publicRegistryUtils,
                                     PublicRegistry publicRegistry,
                                     NotificationUtils notificationUtils) {
        this.publicRegistryUtils = publicRegistryUtils;
        this.publicRegistry = publicRegistry;
        this.notificationUtils = notificationUtils;
    }

    /**
     * Send get request to public registry for get digital address
     **/
    public void sendRequestForGetDigitalGeneralAddress(NotificationInt notification, Integer recIndex, ContactPhaseInt contactPhase, int sentAttemptMade) {

        String correlationId = publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, contactPhase, sentAttemptMade, DeliveryModeInt.DIGITAL);
        log.info("SendRequestForGetDigitalAddress correlationId {} - iun {} id {}", correlationId, notification.getIun(), recIndex);
        
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        publicRegistry.sendRequestForGetDigitalAddress(recipient.getTaxId(), correlationId);
        publicRegistryUtils.addPublicRegistryCallToTimeline(notification, recIndex, contactPhase, sentAttemptMade, correlationId, DeliveryModeInt.DIGITAL);

        log.debug("End sendRequestForGetAddress correlationId {} - iun {} id {}", correlationId, notification.getIun(), recIndex);
    }

    /**
     * Send get request to public registry for physical address
     **/
    public void sendRequestForGetPhysicalAddress(NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        String correlationId = publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, ContactPhaseInt.SEND_ATTEMPT, sentAttemptMade, DeliveryModeInt.ANALOG);
        log.info("SendRequestForGetPhysicalAddress correlationId {} - iun {} id {}", correlationId, notification.getIun(), recIndex);
        
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        publicRegistry.sendRequestForGetPhysicalAddress(recipient.getTaxId(), correlationId);
        publicRegistryUtils.addPublicRegistryCallToTimeline(notification, recIndex, ContactPhaseInt.SEND_ATTEMPT, sentAttemptMade, correlationId, DeliveryModeInt.ANALOG);

        log.debug("End sendRequestForGetPhysicalAddress correlationId {} - iun {} id {}", correlationId, notification.getIun(), recIndex);
    }
}
