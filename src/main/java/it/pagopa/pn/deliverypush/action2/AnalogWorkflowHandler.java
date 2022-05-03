package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action2.utils.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnalogWorkflowHandler {
    private final NotificationService notificationService;
    private final ExternalChannelSendHandler externalChannelSendHandler;
    private final CompletionWorkFlowHandler completionWorkFlow;
    private final AnalogWorkflowUtils analogWorkflowUtils;
    private final PublicRegistrySendHandler publicRegistrySendHandler;
    private final InstantNowSupplier instantNowSupplier;

    public AnalogWorkflowHandler(NotificationService notificationService, ExternalChannelSendHandler externalChannelSendHandler,
                                 CompletionWorkFlowHandler completionWorkFlow, AnalogWorkflowUtils analogWorkflowUtils,
                                 PublicRegistrySendHandler publicRegistrySendHandler, InstantNowSupplier instantNowSupplier) {
        this.notificationService = notificationService;
        this.externalChannelSendHandler = externalChannelSendHandler;
        this.completionWorkFlow = completionWorkFlow;
        this.analogWorkflowUtils = analogWorkflowUtils;
        this.publicRegistrySendHandler = publicRegistrySendHandler;
        this.instantNowSupplier = instantNowSupplier;
    }

    public void startAnalogWorkflow(String iun, int recIndex) {
        Notification notification = notificationService.getNotificationByIun(iun);
        nextWorkflowStep(notification, recIndex, 0);
    }

    /**
     * Handle analog notification Workflow based on already made attempt
     */
    public void nextWorkflowStep(Notification notification, int recIndex, int sentAttemptMade) {
        log.info("Start Analog next workflow action - iun {} id {}", notification.getIun(), recIndex);
        
        String iun = notification.getIun();
        log.debug("Sent attempt made is {} - iun {} id {}", sentAttemptMade, iun, recIndex);

        switch (sentAttemptMade) {
            case 0:
                log.info("Handle first send attempt - iun {} id {}", iun, recIndex);

                PhysicalAddress paProvidedAddress = analogWorkflowUtils.getPhysicalAddress(notification, recIndex);

                if (paProvidedAddress != null) {
                    log.info("Start send notification with Pa address - iun {} id {}", iun, recIndex);
                    //send notification with paAddress
                    externalChannelSendHandler.sendAnalogNotification(notification, paProvidedAddress, recIndex, true, sentAttemptMade);
                } else {
                    log.info("Pa address is not available, need to get address from public registry - iun {} id {}", iun, recIndex);
                    //Get address for notification from public registry
                    publicRegistrySendHandler.sendRequestForGetPhysicalAddress(notification, recIndex, sentAttemptMade);
                }
                break;
            case 1:
                log.info("Handle second attempt, send request to public registry - iun {} id {}", iun, recIndex);
                //Send attempt was already made, get address from public registry for second send attempt
                publicRegistrySendHandler.sendRequestForGetPhysicalAddress(notification, recIndex, sentAttemptMade);
                break;
            case 2:
                // All sent attempts have been made. The user is not reachable
                log.info("User with iun {} and id {} is unreachable, all attempt was failed", iun, recIndex);
                completionWorkFlow.completionAnalogWorkflow(notification, recIndex, instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
                break;
            default:
                handleAttemptError(iun, recIndex, sentAttemptMade);
        }
    }

    /**
     * Handle get response for public registry call.
     */
    public void handlePublicRegistryResponse(String iun, int recIndex, PublicRegistryResponse response, int sentAttemptMade) {
        log.info("Handle analog public registry response sentAttemptMade {} - iun {} id {} ", sentAttemptMade, iun, recIndex);

        Notification notification = notificationService.getNotificationByIun(iun);

        switch (sentAttemptMade) {
            case 0:
                log.info("Public registry response is for first attempt  - iun {} id {}", iun, recIndex);
                checkAddressAndSend(notification, recIndex, response.getPhysicalAddress(), true, sentAttemptMade);
                break;
            case 1:
                log.info("Public registry response is for second attempt  - iun {} id {}", iun, recIndex);
                publicRegistrySecondSendResponse(response, notification, recIndex, sentAttemptMade);
                break;
            default:
                handleAttemptError(iun, recIndex, sentAttemptMade);
        }
    }
    
    private void handleAttemptError(String iun, int recIndex, int sentAttemptMade) {
        log.error("Specified attempt {} is not possibile  - iun {} id {}", sentAttemptMade, iun, recIndex);
        throw new PnInternalException("Specified attempt " + sentAttemptMade + " is not possibile");
    }

    private void publicRegistrySecondSendResponse(PublicRegistryResponse response, Notification notification, int recIndex, int sentAttemptMade) {
        String iun = notification.getIun();
        log.info("Start publicRegistrySecondSendResponse  - iun {} id {}", iun, recIndex);

        //Vengono ottenute le informazioni del primo invio effettuato tramite external channel dalla timeline
        SendPaperFeedbackDetails lastSentFeedback = analogWorkflowUtils.getLastTimelineSentFeedback(iun, recIndex);
        log.debug("getLastTimelineSentFeedback completed  - iun {} id {}", iun, recIndex);

        //Se l'indirizzo fornito da public registry è presente ...
        if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {

            PhysicalAddress lastUsedAddress = lastSentFeedback.getAddress();

            //... e risulta diverso da quello utilizzato nel primo tentativo, viene inviata seconda notifica ad external channel con questo indirizzo
            if (!response.getPhysicalAddress().equals(lastUsedAddress)) {
                log.info("Send second notification to external channel with public registry response address  - iun {} id {}", iun, recIndex);
                externalChannelSendHandler.sendAnalogNotification(notification, response.getPhysicalAddress(), recIndex, false, sentAttemptMade);
            } else {
                log.info("First send address and public registry response address are equals  - iun {} id {}", iun, recIndex);
                //... se i due indirizzi sono uguali, viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
                sendWithInvestigationAddress(notification, recIndex, sentAttemptMade, lastSentFeedback.getNewAddress());
            }
        } else {
            log.info("Public registry response address is empty  - iun {} id {}", iun, recIndex);
            //Viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
            sendWithInvestigationAddress(notification, recIndex, sentAttemptMade, lastSentFeedback.getNewAddress());
        }
    }

    private void sendWithInvestigationAddress(Notification notification, int recIndex, int sentAttemptMade, PhysicalAddress newAddress) {
        log.info("Check address from investigation");
        checkAddressAndSend(notification, recIndex, newAddress, false, sentAttemptMade);
    }

    private void checkAddressAndSend(Notification notification, int recIndex, PhysicalAddress address, boolean investigation, int sentAttemptMade) {
        //Se l'indirizzo passato è valorizzato viene inviata la notifica ad externalChannel...
        if (address != null && address.getAddress() != null) {
            log.info("Have a valid address, send notification to external channel  - iun {} id {}", notification.getIun(), recIndex);
            externalChannelSendHandler.sendAnalogNotification(notification, address, recIndex, investigation, sentAttemptMade);
        } else {
            //... se l'indirizzo non è presente non è possibile raggiungere il destinatario che risulta irreperibile 
            log.info("Address isn't valid, user is unreachable  - iun {} id {}", notification.getIun(), recIndex);
            completionWorkFlow.completionAnalogWorkflow(notification, recIndex, instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
        }
    }

    public void extChannelResponseHandler(ExtChannelResponse response, TimelineElement notificationTimelineElement) {
        SendPaperDetails sendPaperDetails = (SendPaperDetails) notificationTimelineElement.getDetails();

        String iun = response.getIun();
        Notification notification = notificationService.getNotificationByIun(iun);
        int recIndex = sendPaperDetails.getRecIndex();
        
        log.info("Analog workflow Ext channel response  - iun {} id {} with status {}", iun, recIndex, response.getResponseStatus());

        if (response.getResponseStatus() != null) {
            switch (response.getResponseStatus()) {
                case OK:
                    // La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                    completionWorkFlow.completionAnalogWorkflow(notification, recIndex, response.getNotificationDate(), sendPaperDetails.getAddress(), EndWorkflowStatus.SUCCESS);
                    break;
                case KO:
                    // External channel non è riuscito a effettuare la notificazione, si passa al prossimo step del workflow
                    int sentAttemptMade = sendPaperDetails.getSentAttemptMade() + 1;
                    analogWorkflowUtils.addAnalogFailureAttemptToTimeline(response, sentAttemptMade, sendPaperDetails);
                    nextWorkflowStep(notification, recIndex, sentAttemptMade);
                    break;
                default:
                    handleStatusError(response, iun, recIndex);
            }
        } else {
            handleStatusError(response, iun, recIndex);
        }

    }

    private void handleStatusError(ExtChannelResponse response, String iun, int recIndex) {
        log.error("Specified response {} is not possibile  - iun {} id {}", response.getResponseStatus(), iun, recIndex);
        throw new PnInternalException("Specified response " + response.getResponseStatus() + " is not possibile");
    }

}
