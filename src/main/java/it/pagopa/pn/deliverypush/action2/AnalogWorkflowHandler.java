package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.DiscoveredAddress;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action2.utils.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AnalogWorkflowHandler {

    private final NotificationService notificationService;
    private final ExternalChannelSendHandler externalChannelSendHandler;
    private final CompletionWorkFlowHandler completionWorkFlow;
    private final AnalogWorkflowUtils analogWorkflowUtils;
    private final PublicRegistrySendHandler publicRegistrySendHandler;
    private final InstantNowSupplier instantNowSupplier;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    public AnalogWorkflowHandler(NotificationService notificationService, ExternalChannelSendHandler externalChannelSendHandler,
                                 CompletionWorkFlowHandler completionWorkFlow, AnalogWorkflowUtils analogWorkflowUtils,
                                 PublicRegistrySendHandler publicRegistrySendHandler, InstantNowSupplier instantNowSupplier, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.notificationService = notificationService;
        this.externalChannelSendHandler = externalChannelSendHandler;
        this.completionWorkFlow = completionWorkFlow;
        this.analogWorkflowUtils = analogWorkflowUtils;
        this.publicRegistrySendHandler = publicRegistrySendHandler;
        this.instantNowSupplier = instantNowSupplier;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    public void startAnalogWorkflow(String iun, Integer recIndex) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        nextWorkflowStep(notification, recIndex, 0);
    }

    /**
     * Handle analog notification Workflow based on already made attempt
     */
    public void nextWorkflowStep(NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        log.info("Start Analog next workflow action - iun={} id={}", notification.getIun(), recIndex);
        
        String iun = notification.getIun();
        log.debug("Sent attempt made is={} - iun={} id={}", sentAttemptMade, iun, recIndex);

        switch (sentAttemptMade) {
            case 0:
                log.info("Handle first send attempt - iun={} id={}", iun, recIndex);

                PhysicalAddress paProvidedAddress = analogWorkflowUtils.getPhysicalAddress(notification, recIndex);

                if (paProvidedAddress != null) {
                    log.info("Start send notification with Pa address - iun={} id={}", iun, recIndex);
                    //send notification with paAddress
                    externalChannelSendHandler.sendAnalogNotification(notification, paProvidedAddress, recIndex, true, sentAttemptMade);
                } else {
                    log.info("Pa address is not available, need to get address from public registry - iun={} id={}", iun, recIndex);
                    //Get address for notification from public registry
                    publicRegistrySendHandler.sendRequestForGetPhysicalAddress(notification, recIndex, sentAttemptMade);
                }
                break;
            case 1:
                log.info("Handle second attempt, send request to public registry - iun={} id={}", iun, recIndex);
                //Send attempt was already made, get address from public registry for second send attempt
                publicRegistrySendHandler.sendRequestForGetPhysicalAddress(notification, recIndex, sentAttemptMade);
                break;
            case 2:
                // All sent attempts have been made. The user is not reachable
                log.info("User with iun={} and id={} is unreachable, all attempt was failed", iun, recIndex);
                completionWorkFlow.completionAnalogWorkflow(notification, recIndex, null, instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
                break;
            default:
                handleAttemptError(iun, recIndex, sentAttemptMade);
        }
    }

    /**
     * Handle get response for public registry call.
     */
    public void handlePublicRegistryResponse(String iun, Integer recIndex, PublicRegistryResponse response, int sentAttemptMade) {
        log.info("Handle analog public registry response sentAttemptMade={} - iun={} id={} ", sentAttemptMade, iun, recIndex);

        NotificationInt notification = notificationService.getNotificationByIun(iun);

        switch (sentAttemptMade) {
            case 0:
                log.info("Public registry response is for first attempt  - iun={} id={}", iun, recIndex);
                checkAddressAndSend(notification, recIndex, response.getPhysicalAddress(), true, sentAttemptMade);
                break;
            case 1:
                log.info("Public registry response is for second attempt  - iun={} id={}", iun, recIndex);
                publicRegistrySecondSendResponse(response, notification, recIndex, sentAttemptMade);
                break;
            default:
                handleAttemptError(iun, recIndex, sentAttemptMade);
        }
    }
    
    private void handleAttemptError(String iun, Integer recIndex, int sentAttemptMade) {
        log.error("Specified attempt={} is not possibile  - iun={} id={}", sentAttemptMade, iun, recIndex);
        throw new PnInternalException("Specified attempt " + sentAttemptMade + " is not possibile");
    }

    private void publicRegistrySecondSendResponse(PublicRegistryResponse response, NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        String iun = notification.getIun();
        log.info("Start publicRegistrySecondSendResponse  - iun={} id={}", iun, recIndex);

        //Vengono ottenute le informazioni del primo invio effettuato tramite external channel dalla timeline
        SendPaperFeedbackDetails lastSentFeedback = analogWorkflowUtils.getLastTimelineSentFeedback(iun, recIndex);
        log.debug("getLastTimelineSentFeedback completed  - iun={} id={}", iun, recIndex);

        //Se l'indirizzo fornito da public registry è presente ...
        if (response.getPhysicalAddress() != null) {

            PhysicalAddress lastUsedAddress = lastSentFeedback.getPhysicalAddress();

            //... e risulta diverso da quello utilizzato nel primo tentativo, viene inviata seconda notifica ad external channel con questo indirizzo
            if (!response.getPhysicalAddress().equals(lastUsedAddress)) {
                log.info("Send second notification to external channel with public registry response address  - iun={} id={}", iun, recIndex);
                externalChannelSendHandler.sendAnalogNotification(notification, response.getPhysicalAddress(), recIndex, false, sentAttemptMade);
            } else {
                log.info("First send address and public registry response address are equals  - iun={} id={}", iun, recIndex);
                //... se i due indirizzi sono uguali, viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
                sendWithInvestigationAddress(notification, recIndex, sentAttemptMade, lastSentFeedback.getNewAddress());
            }
        } else {
            log.info("Public registry response address is empty  - iun={} id={}", iun, recIndex);
            //Viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
            sendWithInvestigationAddress(notification, recIndex, sentAttemptMade, lastSentFeedback.getNewAddress());
        }
    }

    private void sendWithInvestigationAddress(NotificationInt notification, Integer recIndex, int sentAttemptMade, PhysicalAddress newAddress) {
        log.info("Check address from investigation");
        checkAddressAndSend(notification, recIndex, newAddress, false, sentAttemptMade);
    }

    private void checkAddressAndSend(NotificationInt notification, Integer recIndex, PhysicalAddress address, boolean investigation, int sentAttemptMade) {
        //Se l'indirizzo passato è valorizzato viene inviata la notifica ad externalChannel...
        if (address != null) {
            log.info("Have a valid address, send notification to external channel  - iun={} id={}", notification.getIun(), recIndex);
            externalChannelSendHandler.sendAnalogNotification(notification, address, recIndex, investigation, sentAttemptMade);
        } else {
            //... se l'indirizzo non è presente non è possibile raggiungere il destinatario che risulta irreperibile 
            log.info("Address isn't valid, user is unreachable  - iun={} id={}", notification.getIun(), recIndex);
            completionWorkFlow.completionAnalogWorkflow(notification, recIndex, null, instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
        }
    }

    public void extChannelResponseHandler(PaperProgressStatusEvent response, TimelineElementInternal notificationTimelineElement) {
        SendPaperDetails sendPaperDetails = SmartMapper.mapToClass(notificationTimelineElement.getDetails(), SendPaperDetails.class);

        String iun = response.getIun();
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        Integer recIndex = sendPaperDetails.getRecIndex();
        ResponseStatus status = mapPaperStatusInResponseStatus(response.getStatusCode());
        List<LegalFactsId> legalFactsListEntryIds;
        if ( response.getAttachments() != null ) {
            legalFactsListEntryIds = response.getAttachments().stream()
                    .map( k -> LegalFactsId.builder()
                            .key( k.getUrl() )
                            .category( LegalFactCategory.ANALOG_DELIVERY )
                            .build()
                    ).collect(Collectors.toList());
        } else {
            legalFactsListEntryIds = Collections.emptyList();
        }

        log.info("Analog workflow Ext channel response  - iun={} id={} with status={}", iun, recIndex, response.getStatusCode());

        if (status!= null) {
            switch (status) {
                case OK:
                    // La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                    completionWorkFlow.completionAnalogWorkflow(notification, recIndex, legalFactsListEntryIds, response.getStatusDateTime(), sendPaperDetails.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);
                    break;
                case KO:
                    // External channel non è riuscito a effettuare la notificazione, si passa al prossimo step del workflow
                    int sentAttemptMade = sendPaperDetails.getSentAttemptMade() + 1;
                    PhysicalAddress newPhysicalAddress = null;
                    if (response.getDiscoveredAddress() != null)
                    {
                        DiscoveredAddress rawAddress = response.getDiscoveredAddress();
                        newPhysicalAddress = PhysicalAddress.builder()
                                .address(rawAddress.getAddress())
                                .addressDetails(rawAddress.getAddressRow2())
                                .municipality(rawAddress.getCity())
                                .municipalityDetails(rawAddress.getCity2())
                                .province(rawAddress.getPr())
                                .zip(rawAddress.getCap())
                                .foreignState(rawAddress.getCountry())
                                .at(rawAddress.getNameRow2())
                                .build();
                    }
                    analogWorkflowUtils.addAnalogFailureAttemptToTimeline(iun, sentAttemptMade, legalFactsListEntryIds, newPhysicalAddress, response.getDeliveryFailureCause()==null?null:List.of(response.getDeliveryFailureCause()),  sendPaperDetails);
                    nextWorkflowStep(notification, recIndex, sentAttemptMade);
                    break;
            }
        } else {
            handleStatusProgress(response, iun, recIndex);
        }

    }

    private void handleStatusProgress(PaperProgressStatusEvent event, String iun, Integer recIndex) {
        log.error("Specified response={} is not final  - iun={} id={}", event.getStatusCode(), iun, recIndex);
    }

    private ResponseStatus mapPaperStatusInResponseStatus(String paperStatus)
    {
        /* Codifica sintetica dello stato dell'esito._  <br/>
                - __001__ Stampato  <br/>
                - __002__ Disponibile al recapitista  <br/>
                - __003__ Preso in carico dal recapitista  <br/>
                - __004__ Consegnata  <br/>
                - __005__ Mancata consegna  <br/>
                - __006__ Furto/Smarrimanto/deterioramento  <br/>
                - __007__ Consegnato Ufficio Postale  <br/>
                - __008__ Mancata consegna Ufficio Postale  <br/>
                - __009__ Compiuta giacenza  <br/>
        */
        if (paperStatus == null)
            throw new PnInternalException("Invalid received paper status:" + paperStatus);

        if (this.pnDeliveryPushConfigs.getExternalChannel().getAnalogCodesProgress().contains(paperStatus)){
            return null;
        }
        if (this.pnDeliveryPushConfigs.getExternalChannel().getAnalogCodesSuccess().contains(paperStatus)){
            return ResponseStatus.OK;
        }
        if (this.pnDeliveryPushConfigs.getExternalChannel().getAnalogCodesFail().contains(paperStatus)){
            return  ResponseStatus.KO;
        }

        throw new PnInternalException("Invalid received paper status:" + paperStatus);
    }

}
