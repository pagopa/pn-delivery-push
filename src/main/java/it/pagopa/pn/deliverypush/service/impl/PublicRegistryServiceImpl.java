package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.deliverypush.external.PublicRegistry;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PublicRegistryServiceImpl implements PublicRegistryService {
    private final PublicRegistry publicRegistry;
    private final TimelineService timelineService;

    public PublicRegistryServiceImpl(PublicRegistry publicRegistry, TimelineService timelineService) {
        this.publicRegistry = publicRegistry;
        this.timelineService = timelineService;
    }

    /**
     * Send get request to public registry
     *
     * @param iun           Notification unique identifier
     * @param taxId         User identifier
     * @param correlationId requestId
     * @param deliveryMode  DIGITAL,ANALOG or null in choose phase
     * @param contactPhase  Process phase where the request is sent. CHOOSE_DELIVERY -> request sent during delivery selection,  SEND_ATTEMPT ->  request Sent in Digital or Analogic workflow
     */
    @Override
    public void sendRequestForGetDigitalAddress(String iun, String taxId, ContactPhase contactPhase, int sentAttemptMade) {
        log.info("Start sendRequestForGetDigitalAddress for IUN {} id {} ", iun, taxId);

        String correlationId = String.format(
                "%s_%s_%s_%s_%d",
                iun,
                taxId,
                DeliveryMode.DIGITAL,
                contactPhase,
                sentAttemptMade
        );

        publicRegistry.sendRequestForGetDigitalAddress(taxId, correlationId);
        timelineService.addPublicRegistryCallToTimeline(iun, taxId, correlationId, DeliveryMode.DIGITAL, contactPhase, sentAttemptMade);

        log.debug("End sendRequestForGetAddress for IUN {} id {} correlationId {}", iun, taxId, correlationId);
    }

    @Override
    public void sendRequestForGetPhysicalAddress(String iun, String taxId, int sentAttemptMade) {
        log.info("Start sendRequestForGetPhysicalAddress for IUN {} id {} ", iun, taxId);

        String correlationId = String.format(
                "%s_%s_%s_%s_%d",
                iun,
                taxId,
                DeliveryMode.ANALOG,
                ContactPhase.SEND_ATTEMPT,
                sentAttemptMade
        );

        publicRegistry.sendRequestForGetPhysicalAddress(taxId, correlationId);
        timelineService.addPublicRegistryCallToTimeline(iun, taxId, correlationId, DeliveryMode.ANALOG, ContactPhase.SEND_ATTEMPT, sentAttemptMade);

        log.debug("End sendRequestForGetPhysicalAddress for IUN {} id {} correlationId {}", iun, taxId, correlationId);
    }
}
