package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.deliverypush.external.PublicRegistry;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PublicRegistryServiceImpl implements PublicRegistryService {
    private PublicRegistry publicRegistry;
    private TimelineService timelineService;

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
    public void sendRequestForGetAddress(String iun, String taxId, String correlationId, DeliveryMode deliveryMode, ContactPhase contactPhase) {
        log.debug("Start sendRequestForGetAddress for IUN {} id {} correlationId {}", iun, taxId, correlationId);

        publicRegistry.sendRequest(iun, taxId);
        addPublicRegistryCallToTimeline(iun, taxId, correlationId, deliveryMode, contactPhase); //L'inserimento in timeline ha senso portarlo in publicRegistrySender

        log.debug("End sendRequestForGetAddress for IUN {} id {} correlationId {}", iun, taxId, correlationId);
    }

    private void addPublicRegistryCallToTimeline(String iun, String taxId, String correlationId, DeliveryMode deliveryMode, ContactPhase contactPhase) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .iun(iun)
                .elementId(correlationId)
                .category(TimelineElementCategory.PUBLIC_REGISTRY_CALL)
                .details(PublicRegistryCallDetails.builder()
                        .taxId(taxId)
                        .contactPhase(contactPhase)
                        .deliveryMode(deliveryMode)
                        .build())
                .build());
    }
}
