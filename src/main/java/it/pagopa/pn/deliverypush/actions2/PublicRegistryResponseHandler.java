package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.notification.timeline.PublicRegistryCallDetails;
import it.pagopa.pn.api.dto.notification.timeline.PublicRegistryResponseDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;

import java.util.Optional;

@Slf4j
public class PublicRegistryResponseHandler {
    private TimelineService timelineService;
    private ChooseDeliveryModeHandler chooseDeliveryHandler;
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    private AnalogWorkflowHandler analogWorkflowHandler;

    /**
     * Handle response from get request to public registry
     *
     * @param response public registry response
     */
    @StreamListener(condition = "PUBLIC_REGISTRY_RESPONSE")
    public void handleResponse(PublicRegistryResponse response) {
        log.info("Start handleResponse for correlationId {}", response.getCorrelationId());

        String correlationId = response.getCorrelationId();
        String iun = correlationId.substring(0, correlationId.indexOf("_") - 1); //TODO Da modificare quando verrà risolta PN-533

        //Viene ottenuto l'oggetto di timeline creato in fase di invio notifica al public registry
        Optional<PublicRegistryCallDetails> optTimeLinePublicRegistrySend = timelineService.getTimelineElement(iun, response.getCorrelationId(), PublicRegistryCallDetails.class);

        if (optTimeLinePublicRegistrySend.isPresent()) {
            PublicRegistryCallDetails publicRegistryCallDetails = optTimeLinePublicRegistrySend.get();
            String taxId = publicRegistryCallDetails.getTaxId();

            addPublicResponseCallToTimeline(iun, taxId, response);

            log.debug(" timelineElement is present, id {} contactPhase {}", taxId, publicRegistryCallDetails.getContactPhase());

            //In base alla fase di contatto, inserita in timeline al momento dell'invio, viene scelto il percorso da prendere
            switch (publicRegistryCallDetails.getContactPhase()) {
                case CHOOSE_DELIVERY:
                    //request has been sent during delivery selection
                    chooseDeliveryHandler.handleGeneralAddressResponse(response, iun, taxId);
                    break;
                case SEND_ATTEMPT:
                    //request has been sent in digital or analog workflow
                    handleResponseForSendAttempt(response, iun, publicRegistryCallDetails, taxId);
                    break;
                default:
                    log.error("Specified contactPhase {} does not exist for correlationId {}", publicRegistryCallDetails.getContactPhase(), correlationId);
                    throw new PnInternalException("Specified contactPhase " + publicRegistryCallDetails.getContactPhase() + " does not exist for correlationId " + correlationId);
            }

        } else {
            log.error("There isn't timelineElement for iun {} correlationId {}", iun, correlationId);
            throw new PnInternalException("There isn't notification for iun " + iun + " correlationId " + correlationId);
        }
    }

    private void addPublicResponseCallToTimeline(String iun, String taxId, PublicRegistryResponse response) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .iun(iun)
                .elementId(response.getCorrelationId())
                .category(TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE)
                .details(PublicRegistryResponseDetails.builder()
                        .taxId(taxId)
                        .digitalAddress(response.getDigitalAddress())
                        .physicalAddress(response.getPhysicalAddress())
                        .build())
                .build());
    }

    private void handleResponseForSendAttempt(PublicRegistryResponse response, String iun, PublicRegistryCallDetails publicRegistryCallDetails, String taxId) {
        switch (publicRegistryCallDetails.getDeliveryMode()) {
            case DIGITAL:
                digitalWorkFlowHandler.handleGeneralAddressResponse(response, iun, publicRegistryCallDetails.getTaxId());
                break;
            case ANALOG:
                analogWorkflowHandler.handlePublicRegistryResponse(iun, taxId, response);
                break;
            default:
                log.error("Specified deliveryMode {} does not exist for iun {} id {}", publicRegistryCallDetails.getDeliveryMode(), iun, taxId);
                throw new PnInternalException("Specified deliveryMode " + publicRegistryCallDetails.getDeliveryMode() + " does not exist for iun " + iun + " id " + taxId);
        }
    }

}
