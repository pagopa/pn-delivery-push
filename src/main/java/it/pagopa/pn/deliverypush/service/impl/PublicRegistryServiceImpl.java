package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.publicregistry.PublicRegistry;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PublicRegistryServiceImpl implements PublicRegistryService {
    private final PublicRegistryUtils publicRegistryUtils;
    private final PublicRegistry publicRegistry;
    private final NotificationUtils notificationUtils;

    public PublicRegistryServiceImpl(PublicRegistryUtils publicRegistryUtils,
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
        log.debug("Start Async Request for get general address, correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        publicRegistryUtils.addPublicRegistryCallToTimeline(notification, recIndex, contactPhase, sentAttemptMade, correlationId, DeliveryModeInt.DIGITAL);
        publicRegistry.sendRequestForGetDigitalAddress(recipient.getTaxId(), recipient.getRecipientType().getValue(), correlationId);

        log.debug("End sendRequestForGetAddress correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);
    }

    /**
     * Send get request to public registry for physical address
     **/
    public void sendRequestForGetPhysicalAddress(NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        String correlationId = publicRegistryUtils.generateCorrelationId(notification.getIun(), recIndex, ContactPhaseInt.SEND_ATTEMPT, sentAttemptMade, DeliveryModeInt.ANALOG);
        log.info("SendRequestForGetPhysicalAddress correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        publicRegistryUtils.addPublicRegistryCallToTimeline(notification, recIndex, ContactPhaseInt.SEND_ATTEMPT, sentAttemptMade, correlationId, DeliveryModeInt.ANALOG);
        publicRegistry.sendRequestForGetPhysicalAddress(recipient.getTaxId(), correlationId);

        log.debug("End sendRequestForGetPhysicalAddress correlationId={} - iun={} id={}", correlationId, notification.getIun(), recIndex);
    }
}
