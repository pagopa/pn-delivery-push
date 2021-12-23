package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import org.springframework.cloud.stream.annotation.StreamListener;

import java.util.Optional;

public class PublicRegistryHandlerImpl implements PublicRegistryHandler {
    private TimelineDao timelineDao;
    private TimelineService timelineService;
    private CourtesyMessageHandler courtesyMessageHandler;
    private ChooseDeliveryMode chooseDeliveryMode;
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    private AnalogWorkflowHandler analogWorkflowHandler;

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
        //TODO Invia notifica a public registry da implementare
        addPublicRegistryCallToTimeline(iun, taxId, correlationId, deliveryMode, contactPhase); //L'inserimento in timeline ha senso portarlo in publicRegistrySender
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


    /**
     * Handle response from get request to public registry
     *
     * @param response public registry response
     */
    @Override
    @StreamListener(condition = "PUBLIC_REGISTRY_RESPONSE")
    public void handleResponse(PublicRegistryResponse response) {
        String correlationId = response.getCorrelationId();
        String iun = correlationId.substring(0, correlationId.indexOf("_") - 1); //ottiene lo iun dal correlation id
        //TODO Effettuo salvataggio risposta in timeline

        //Get timeline created at sent time
        Optional<PublicRegistryCallDetails> optTimeLinePublicRegistrySend = timelineService.getTimelineElement(iun, response.getCorrelationId(), PublicRegistryCallDetails.class);

        if (optTimeLinePublicRegistrySend.isPresent()) {
            PublicRegistryCallDetails publicRegistryCallDetails = optTimeLinePublicRegistrySend.get();
            String taxId = publicRegistryCallDetails.getTaxId();

            switch (publicRegistryCallDetails.getContactPhase()) {
                case CHOOSE_DELIVERY:
                    //request has been sent during delivery selection
                    chooseDeliveryMode.handleSpecialAddressResponse(response, iun, publicRegistryCallDetails.getTaxId());
                    break;
                case SEND_ATTEMPT:
                    //request has been sent in digital or analog workflow
                    handleResponseForSendAttempt(response, iun, publicRegistryCallDetails, taxId);
                    break;
                default:
                    //TODO GESTIRE ERRORE
            }

        } else {
            //TODO Gestire la casistica di errore, E' stata ricevuta una risposta per un elemento non presente in timeline
        }
    }

    private void handleResponseForSendAttempt(PublicRegistryResponse response, String iun, PublicRegistryCallDetails publicRegistryCallDetails, String taxId) {
        switch (publicRegistryCallDetails.getDeliveryMode()) {
            case DIGITAL:
                digitalWorkFlowHandler.handleSpecialAddressResponse(response, iun, publicRegistryCallDetails.getTaxId());
                break;
            case ANALOG:
                analogWorkflowHandler.handlePublicRegistryResponse(iun, taxId, response);
                break;
            default:
                //TODO GESTIRE ERRORE
        }
    }
    
    
/*
    private boolean selectSendTimeline(TimelineElement el, String taxId) {
        if (TimelineElementCategory.PUBLIC_REGISTRY_CALL.equals(el.getCategory())) {
            PublicRegistryCallDetails details = (PublicRegistryCallDetails) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    private boolean selectSendTimeline(TimelineElement el, NotificationRecipient recipient) {
        if (TimelineElementCategory.SEND_DIGITAL_DOMICILE.equals(el.getCategory())) {
            SendDigitalDetails details = (SendDigitalDetails) el.getDetails();
            return recipient.getTaxId().equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }*/
}
