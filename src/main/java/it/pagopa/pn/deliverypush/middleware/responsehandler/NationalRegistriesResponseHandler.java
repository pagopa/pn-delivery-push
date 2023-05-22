package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.DeliveryModeInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.utils.PublicRegistryUtils;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_CONTACTPHASENOTFOUND;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_DELIVERYNOTFOUND;

@Component
@CustomLog
public class NationalRegistriesResponseHandler {
    private final ChooseDeliveryModeHandler chooseDeliveryHandler;
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final PublicRegistryUtils publicRegistryUtils;
    private final NotificationService notificationService;
    private final TimelineUtils timelineUtils;

    public NationalRegistriesResponseHandler(ChooseDeliveryModeHandler chooseDeliveryHandler,
                                             DigitalWorkFlowHandler digitalWorkFlowHandler,
                                             PublicRegistryUtils publicRegistryUtils,
                                             NotificationService notificationService,
                                             TimelineUtils timelineUtils) {
        this.chooseDeliveryHandler = chooseDeliveryHandler;
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.publicRegistryUtils = publicRegistryUtils;
        this.notificationService = notificationService;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Handle response for request to public registry
     *
     * @param response public registry response
     */
    public void handleResponse(NationalRegistriesResponse response) {
        String correlationId = response.getCorrelationId();
        String iun = timelineUtils.getIunFromTimelineId(correlationId);
        addMdcFilter(iun, correlationId);

        log.info("Async response received from service {} for {} with correlationId={}",
                NationalRegistriesClient.CLIENT_NAME, NationalRegistriesClient.GET_DIGITAL_GENERAL_ADDRESS, correlationId);

        final String processName = NationalRegistriesClient.GET_DIGITAL_GENERAL_ADDRESS + " response handler";
        log.logStartingProcess(processName);
        
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        log.debug("Notification successfully obtained  - iun={}", notification.getIun());

        //Viene ottenuto l'oggetto di timeline creato in fase d'invio notifica al public registry
        PublicRegistryCallDetailsInt publicRegistryCallDetails = publicRegistryUtils.getPublicRegistryCallDetail(iun, correlationId);
        Integer recIndex = publicRegistryCallDetails.getRecIndex();

        publicRegistryUtils.addPublicRegistryResponseToTimeline(notification, recIndex, response);

        handleSpecificContactPhase(response, correlationId, iun, notification, publicRegistryCallDetails, recIndex);

        log.logEndingProcess(processName);
    }

    private void handleSpecificContactPhase(NationalRegistriesResponse response, String correlationId, String iun, 
                                            NotificationInt notification, PublicRegistryCallDetailsInt publicRegistryCallDetails,
                                            Integer recIndex) {
        ContactPhaseInt contactPhase = publicRegistryCallDetails.getContactPhase();
        log.info("public registry response is in contactPhase {} iun {} id {} ", contactPhase, iun, recIndex);

        //In base alla fase di contatto, inserita in timeline al momento dell'invio, viene scelto il percorso da prendere
        if (contactPhase != null) {
            switch (contactPhase) {
                case CHOOSE_DELIVERY ->
                    //request has been sent during choose delivery
                        chooseDeliveryHandler.handleGeneralAddressResponse(response, notification, recIndex);
                case SEND_ATTEMPT ->
                    //request has been sent in digital or analog workflow
                        handleResponseForSendAttempt(response, notification, publicRegistryCallDetails);
                default -> handleContactPhaseError(correlationId, publicRegistryCallDetails);
            }
        } else {
            handleContactPhaseError(correlationId, publicRegistryCallDetails);
        }
    }

    private void handleContactPhaseError(String correlationId, PublicRegistryCallDetailsInt publicRegistryCallDetails) {
        log.error("Specified contactPhase {} does not exist for correlationId {}", publicRegistryCallDetails.getContactPhase(), correlationId);
        throw new PnInternalException("Specified contactPhase " + publicRegistryCallDetails.getContactPhase() + " does not exist for correlationId " + correlationId, ERROR_CODE_DELIVERYPUSH_CONTACTPHASENOTFOUND);
    }

    private void handleResponseForSendAttempt(NationalRegistriesResponse response, 
                                              NotificationInt notification,
                                              PublicRegistryCallDetailsInt publicRegistryCallDetails) {
        Integer recIndex = publicRegistryCallDetails.getRecIndex();
        String iun = notification.getIun();
        
        log.debug("Start handleResponseForSendAttempt iun {} id {} deliveryMode {}", iun, recIndex, publicRegistryCallDetails.getDeliveryMode());

        if (publicRegistryCallDetails.getDeliveryMode() != null) {

            if (publicRegistryCallDetails.getDeliveryMode() == DeliveryModeInt.DIGITAL) {
                digitalWorkFlowHandler.handleGeneralAddressResponse(response, notification, publicRegistryCallDetails);
            } else {
                handleDeliveryModeError(iun, publicRegistryCallDetails.getDeliveryMode(), recIndex);
            }
        } else {
            handleDeliveryModeError(iun, publicRegistryCallDetails.getDeliveryMode(), recIndex);
        }
    }

    private void handleDeliveryModeError(String iun, DeliveryModeInt deliveryMode, Integer recIndex) {
        log.error("Specified deliveryMode {} does not exist - iun {} id {}", deliveryMode, iun, recIndex);
        throw new PnInternalException("Specified deliveryMode " + deliveryMode + " does not exist - iun " + iun + " id " + recIndex, ERROR_CODE_DELIVERYPUSH_DELIVERYNOTFOUND);
    }

    private static void addMdcFilter(String iun, String correlationId) {
        HandleEventUtils.addIunToMdc(iun);
        HandleEventUtils.addCorrelationIdToMdc(correlationId);
    }
}
