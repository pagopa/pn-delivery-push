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
    public void sendRequestForGetAddress(String iun, String taxId, DeliveryMode deliveryMode, ContactPhase contactPhase, int sentAttemptMade) {
        log.debug("Start sendRequestForGetAddress for IUN {} id {} ", iun, taxId);

        String correlationId = String.format(
                "%s_%s_%s_%s_%d",
                iun,
                taxId,
                deliveryMode,
                contactPhase,
                sentAttemptMade
        );

        publicRegistry.sendRequest(iun, taxId);
        timelineService.addPublicRegistryCallToTimeline(iun, taxId, correlationId, deliveryMode, contactPhase, sentAttemptMade); //L'inserimento in timeline ha senso portarlo in publicRegistrySender

        log.debug("End sendRequestForGetAddress for IUN {} id {} correlationId {}", iun, taxId, correlationId);
    }

}
