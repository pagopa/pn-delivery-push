package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.notification.timeline.PublicRegistryCallDetails;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action2.utils.PublicRegistryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PublicRegistryResponseHandler {
    private final ChooseDeliveryModeHandler chooseDeliveryHandler;
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final PublicRegistryUtils publicRegistryUtils;

    public PublicRegistryResponseHandler(ChooseDeliveryModeHandler chooseDeliveryHandler,
                                         DigitalWorkFlowHandler digitalWorkFlowHandler, AnalogWorkflowHandler analogWorkflowHandler,
                                         PublicRegistryUtils publicRegistryUtils) {
        this.chooseDeliveryHandler = chooseDeliveryHandler;
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.publicRegistryUtils = publicRegistryUtils;
    }

    /**
     * Handle response for request to public registry
     *
     * @param response public registry response
     */
    public void handleResponse(PublicRegistryResponse response) {

        String correlationId = response.getCorrelationId();
        String iun = correlationId.substring(0, correlationId.indexOf("_")); //TODO Da modificare quando verrà risolta PN-533
        log.info("Handle public registry response -  iun {} correlationId {}", iun, response.getCorrelationId());

        //Viene ottenuto l'oggetto di timeline creato in fase d'invio notifica al public registry
        PublicRegistryCallDetails publicRegistryCallDetails = publicRegistryUtils.getPublicRegistryCallDetail(iun, correlationId);
        int recIndex = publicRegistryCallDetails.getRecIndex();

        publicRegistryUtils.addPublicRegistryResponseToTimeline(iun, recIndex, response);

        log.info("public registry response is in contactPhase {} iun {} id {} ", publicRegistryCallDetails.getContactPhase(), iun, recIndex);

        ContactPhase contactPhase = publicRegistryCallDetails.getContactPhase();
        //In base alla fase di contatto, inserita in timeline al momento dell'invio, viene scelto il percorso da prendere
        if (contactPhase != null) {
            switch (contactPhase) {
                case CHOOSE_DELIVERY:
                    //request has been sent during choose delivery
                    chooseDeliveryHandler.handleGeneralAddressResponse(response, iun, recIndex);
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

    }

    private void handleContactPhaseError(String correlationId, PublicRegistryCallDetails publicRegistryCallDetails) {
        log.error("Specified contactPhase {} does not exist for correlationId {}", publicRegistryCallDetails.getContactPhase(), correlationId);
        throw new PnInternalException("Specified contactPhase " + publicRegistryCallDetails.getContactPhase() + " does not exist for correlationId " + correlationId);
    }

    private void handleResponseForSendAttempt(PublicRegistryResponse response, String iun, PublicRegistryCallDetails publicRegistryCallDetails) {
        int recIndex = publicRegistryCallDetails.getRecIndex();

        log.info("Start handleResponseForSendAttempt iun {} id {} deliveryMode {}", iun, recIndex, publicRegistryCallDetails.getDeliveryMode());

        if (publicRegistryCallDetails.getDeliveryMode() != null) {

            switch (publicRegistryCallDetails.getDeliveryMode()) {
                case DIGITAL:
                    digitalWorkFlowHandler.handleGeneralAddressResponse(response, iun, publicRegistryCallDetails);
                    break;
                case ANALOG:
                    analogWorkflowHandler.handlePublicRegistryResponse(iun, recIndex, response, publicRegistryCallDetails.getSentAttemptMade());
                    break;
                default:
                    handleDeliveryModeError(iun, publicRegistryCallDetails.getDeliveryMode(), recIndex);
            }
        } else {
            handleDeliveryModeError(iun, publicRegistryCallDetails.getDeliveryMode(), recIndex);
        }
    }

    private void handleDeliveryModeError(String iun, DeliveryMode deliveryMode, int recIndex) {
        log.error("Specified deliveryMode {} does not exist - iun {} id {}", deliveryMode, iun, recIndex);
        throw new PnInternalException("Specified deliveryMode " + deliveryMode + " does not exist - iun " + iun + " id " + recIndex);
    }
}
