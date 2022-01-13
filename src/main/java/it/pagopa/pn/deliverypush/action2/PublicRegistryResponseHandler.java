package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.notification.timeline.PublicRegistryCallDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class PublicRegistryResponseHandler {
    private final TimelineService timelineService;
    private final ChooseDeliveryModeHandler chooseDeliveryHandler;
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final TimelineUtils timelineUtils;

    public PublicRegistryResponseHandler(TimelineService timelineService, ChooseDeliveryModeHandler chooseDeliveryHandler,
                                         DigitalWorkFlowHandler digitalWorkFlowHandler, AnalogWorkflowHandler analogWorkflowHandler,
                                         TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.chooseDeliveryHandler = chooseDeliveryHandler;
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Handle response for request to public registry
     *
     * @param response public registry response
     */
    @StreamListener(condition = "PUBLIC_REGISTRY_RESPONSE")
    public void handleResponse(PublicRegistryResponse response) {

        String correlationId = response.getCorrelationId();
        String iun = correlationId.substring(0, correlationId.indexOf("_")); //TODO Da modificare quando verrà risolta PN-533
        log.info("Handle public registry response -  iun {} correlationId {}", iun, response.getCorrelationId());

        //Viene ottenuto l'oggetto di timeline creato in fase d'invio notifica al public registry
        Optional<PublicRegistryCallDetails> optTimeLinePublicRegistrySend = timelineService.getTimelineElement(iun, response.getCorrelationId(), PublicRegistryCallDetails.class);

        if (optTimeLinePublicRegistrySend.isPresent()) {
            PublicRegistryCallDetails publicRegistryCallDetails = optTimeLinePublicRegistrySend.get();
            String taxId = publicRegistryCallDetails.getTaxId();

            addTimelineElement(timelineUtils.buildPublicRegistryResponseCallTimelineElement(iun, taxId, response));

            log.info("public registry response is in contactPhase {} iun {} id {} ", publicRegistryCallDetails.getContactPhase(), iun, taxId);

            ContactPhase contactPhase = publicRegistryCallDetails.getContactPhase();
            //In base alla fase di contatto, inserita in timeline al momento dell'invio, viene scelto il percorso da prendere
            if (contactPhase != null) {
                switch (contactPhase) {
                    case CHOOSE_DELIVERY:
                        //request has been sent during choose delivery
                        chooseDeliveryHandler.handleGeneralAddressResponse(response, iun, taxId);
                        break;
                    case SEND_ATTEMPT:
                        //request has been sent in digital or analog workflow
                        handleResponseForSendAttempt(response, iun, publicRegistryCallDetails);
                        break;
                    default:
                        handleContactPhaseError(correlationId, publicRegistryCallDetails);
                }
            } else {
                handleContactPhaseError(correlationId, publicRegistryCallDetails);
            }

        } else {
            log.error("There isn't timelineElement - iun {} correlationId {}", iun, correlationId);
            throw new PnInternalException("There isn't timelineElement - iun " + iun + " correlationId " + correlationId);
        }
    }

    private void handleContactPhaseError(String correlationId, PublicRegistryCallDetails publicRegistryCallDetails) {
        log.error("Specified contactPhase {} does not exist for correlationId {}", publicRegistryCallDetails.getContactPhase(), correlationId);
        throw new PnInternalException("Specified contactPhase " + publicRegistryCallDetails.getContactPhase() + " does not exist for correlationId " + correlationId);
    }

    private void handleResponseForSendAttempt(PublicRegistryResponse response, String iun, PublicRegistryCallDetails publicRegistryCallDetails) {
        String taxId = publicRegistryCallDetails.getTaxId();

        log.info("Start handleResponseForSendAttempt iun {} id {} deliveryMode {}", iun, taxId, publicRegistryCallDetails.getDeliveryMode());

        if (publicRegistryCallDetails.getDeliveryMode() != null) {

            switch (publicRegistryCallDetails.getDeliveryMode()) {
                case DIGITAL:
                    digitalWorkFlowHandler.handleGeneralAddressResponse(response, iun, publicRegistryCallDetails);
                    break;
                case ANALOG:
                    analogWorkflowHandler.handlePublicRegistryResponse(iun, taxId, response, publicRegistryCallDetails.getSentAttemptMade());
                    break;
                default:
                    handleError(iun, publicRegistryCallDetails.getDeliveryMode(), taxId);
            }
        } else {
            handleError(iun, publicRegistryCallDetails.getDeliveryMode(), taxId);
        }

    }

    private void handleError(String iun, DeliveryMode deliveryMode, String taxId) {
        log.error("Specified deliveryMode {} does not exist - iun {} id {}", deliveryMode, iun, taxId);
        throw new PnInternalException("Specified deliveryMode " + deliveryMode + " does not exist - iun " + iun + " id " + taxId);
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }
}
