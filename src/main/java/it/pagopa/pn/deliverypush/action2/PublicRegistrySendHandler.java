package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.deliverypush.action2.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.external.PublicRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PublicRegistrySendHandler {
    private final PublicRegistryUtils publicRegistryUtils;
    private final PublicRegistry publicRegistry;

    public PublicRegistrySendHandler(PublicRegistryUtils publicRegistryUtils, PublicRegistry publicRegistry) {
        this.publicRegistryUtils = publicRegistryUtils;
        this.publicRegistry = publicRegistry;
    }

    /**
     * Send get request to public registry for get digital address
     **/
    public void sendRequestForGetDigitalAddress(String iun, String taxId, ContactPhase contactPhase, int sentAttemptMade) {

        String correlationId = publicRegistryUtils.generateCorrelationId(iun, taxId, contactPhase, sentAttemptMade, DeliveryMode.DIGITAL);
        log.info("SendRequestForGetDigitalAddress correlationId {} - iun {} id {}", correlationId, iun, taxId);

        publicRegistryUtils.addPublicRegistryCallToTimeline(iun, taxId, contactPhase, sentAttemptMade, correlationId, DeliveryMode.DIGITAL);
        publicRegistry.sendRequestForGetDigitalAddress(taxId, correlationId);

        log.debug("End sendRequestForGetAddress correlationId {} - iun {} id {}", correlationId, iun, taxId);
    }

    /**
     * Send get request to public registry for physical address
     **/
    public void sendRequestForGetPhysicalAddress(String iun, String taxId, int sentAttemptMade) {
        String correlationId = publicRegistryUtils.generateCorrelationId(iun, taxId, ContactPhase.SEND_ATTEMPT, sentAttemptMade, DeliveryMode.ANALOG);
        log.info("SendRequestForGetPhysicalAddress correlationId {} - iun {} id {}", correlationId, iun, taxId);

        publicRegistryUtils.addPublicRegistryCallToTimeline(iun, taxId, ContactPhase.SEND_ATTEMPT, sentAttemptMade, correlationId, DeliveryMode.ANALOG);
        publicRegistry.sendRequestForGetPhysicalAddress(taxId, correlationId);

        log.debug("End sendRequestForGetPhysicalAddress correlationId {} - iun {} id {}", correlationId, iun, taxId);
    }
}
