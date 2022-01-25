package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action2.utils.AnalogWorkflowUtils;
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

    /**
     * Handle analog notification Workflow based on already made attempt
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    //@StreamListener(condition = "ANALOG_WORKFLOW")
    public void nextWorkflowStep(String iun, String taxId, int sentAttemptMade) {
        log.info("Start Analog next workflow action - iun {} id {}", iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);
        log.debug("Get notification and recipient completed - iun {} id {}", iun, taxId);

        log.debug("Sent attempt made is {} - iun {} id {}", sentAttemptMade, iun, taxId);

        switch (sentAttemptMade) {
            case 0:
                log.info("Handle first send attempt - iun {} id {}", iun, taxId);

                PhysicalAddress paProvidedAddress = recipient.getPhysicalAddress();

                if (paProvidedAddress != null) {
                    log.info("Start send notification with Pa address - iun {} id {}", iun, taxId);
                    //send notification with paAddress
                    externalChannelSendHandler.sendAnalogNotification(notification, paProvidedAddress, recipient, true, sentAttemptMade);
                } else {
                    log.info("Pa address is not available, need to get address from public registry - iun {} id {}", iun, taxId);
                    //Get address for notification from public registry
                    publicRegistrySendHandler.sendRequestForGetPhysicalAddress(iun, taxId, sentAttemptMade);
                }
                break;
            case 1:
                log.info("Handle second attempt, send request to public registry - iun {} id {}", iun, taxId);
                //Send attempt was already made, get address from public registry for second send attempt
                publicRegistrySendHandler.sendRequestForGetPhysicalAddress(iun, taxId, sentAttemptMade);
                break;
            case 2:
                // All sent attempts have been made. The user is not reachable
                log.info("User with iun {} and id {} is unreachable, all attempt was failed", iun, taxId);
                completionWorkFlow.completionAnalogWorkflow(taxId, iun, instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
                break;
            default:
                log.error("Specified attempt {} is not possibile  - iun {} id {}", sentAttemptMade, iun, taxId);
                throw new PnInternalException("Specified attempt " + sentAttemptMade + " is not possibile");
        }
    }

    /**
     * Handle get response for public registry call.
     *
     * @param iun      Notification unique identifier
     * @param taxId    User identifier
     * @param response public registry response
     */
    public void handlePublicRegistryResponse(String iun, String taxId, PublicRegistryResponse response, int sentAttemptMade) {
        log.info("Handle analog public registry response sentAttemptMade {} - iun {} id {} ", sentAttemptMade, iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);

        switch (sentAttemptMade) {
            case 0:
                log.info("Public registry response is for first attempt  - iun {} id {}", iun, taxId);
                checkAddressAndSend(notification, recipient, response.getPhysicalAddress(), true, sentAttemptMade);
                break;
            case 1:
                log.info("Public registry response is for second attempt  - iun {} id {}", iun, taxId);
                publicRegistrySecondSendResponse(response, notification, recipient, sentAttemptMade);
                break;
            default:
                log.error("Specified attempt {} is not possibile  - iun {} id {}", sentAttemptMade, iun, taxId);
                throw new PnInternalException("Specified attempt " + sentAttemptMade + " is not possibile");
        }
    }

    private void publicRegistrySecondSendResponse(PublicRegistryResponse response, Notification notification, NotificationRecipient recipient, int sentAttemptMade) {
        String iun = notification.getIun();
        String taxId = recipient.getTaxId();
        log.info("Start publicRegistrySecondSendResponse  - iun {} id {}", iun, taxId);

        //Vengono ottenute le informazioni del primo invio effettuato tramite external channel dalla timeline
        SendPaperFeedbackDetails lastSentFeedback = analogWorkflowUtils.getLastTimelineSentFeedback(iun, taxId);
        log.debug("getLastTimelineSentFeedback completed  - iun {} id {}", iun, taxId);

        //Se l'indirizzo fornito da public registry è presente ...
        if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {

            PhysicalAddress lastUsedAddress = lastSentFeedback.getAddress();

            //... e risulta diverso da quello utilizzato nel primo tentativo, viene inviata seconda notifica ad external channel con questo indirizzo
            if (!response.getPhysicalAddress().equals(lastUsedAddress)) {
                log.info("Send second notification to external channel with public registry response address  - iun {} id {}", iun, taxId);
                externalChannelSendHandler.sendAnalogNotification(notification, response.getPhysicalAddress(), recipient, false, sentAttemptMade);
            } else {
                log.info("First send address and public registry response address are equals  - iun {} id {}", iun, taxId);
                //... se i due indirizzi sono uguali, viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
                sendWithInvestigationAddress(notification, recipient, sentAttemptMade, lastSentFeedback);
            }
        } else {
            log.info("Public registry response address is empty  - iun {} id {}", iun, taxId);
            //Viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
            sendWithInvestigationAddress(notification, recipient, sentAttemptMade, lastSentFeedback);
        }
    }

    private void sendWithInvestigationAddress(Notification notification, NotificationRecipient recipient, int sentAttemptMade, SendPaperFeedbackDetails lastSentFeedback) {
        log.info("Check address from investigation");
        checkAddressAndSend(notification, recipient, lastSentFeedback.getNewAddress(), false, sentAttemptMade);
    }

    private void checkAddressAndSend(Notification notification, NotificationRecipient recipient, PhysicalAddress address, boolean investigation, int sentAttemptMade) {
        //Se l'indirizzo passato è valorizzato viene inviata la notifica ad external channel...
        if (address != null && address.getAddress() != null) {
            log.info("Have a valid address, send notification to external channel  - iun {} id {}", notification.getIun(), recipient.getTaxId());
            externalChannelSendHandler.sendAnalogNotification(notification, address, recipient, investigation, sentAttemptMade);
        } else {
            //... se l'indirizzo non è presente non è possibile raggiungere il destinario che risulta irreperibile 
            log.info("Address isn't valid, user is unreachable  - iun {} id {}", notification.getIun(), recipient.getTaxId());
            completionWorkFlow.completionAnalogWorkflow(recipient.getTaxId(), notification.getIun(), instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
        }
    }

    public void extChannelResponseHandler(ExtChannelResponse response, TimelineElement notificationTimelineElement) {
        SendPaperDetails sendPaperDetails = (SendPaperDetails) notificationTimelineElement.getDetails();

        String iun = response.getIun();
        String taxId = sendPaperDetails.getTaxId();
        log.info("Analog workflow Ext channel response  - iun {} id {} with status {}", iun, taxId, response.getResponseStatus());

        if (response.getResponseStatus() != null) {
            switch (response.getResponseStatus()) {
                case OK:
                    // La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                    completionWorkFlow.completionAnalogWorkflow(taxId, iun, response.getNotificationDate(), sendPaperDetails.getAddress(), EndWorkflowStatus.SUCCESS);
                    break;
                case KO:
                    // External channel non è riuscito a effettuare la notificazione, si passa al prossimo step del workflow
                    int sentAttemptMade = sendPaperDetails.getSentAttemptMade() + 1;
                    analogWorkflowUtils.addAnalogFailureAttemptToTimeline(response, sentAttemptMade, sendPaperDetails);
                    nextWorkflowStep(iun, taxId, sentAttemptMade);
                    break;
                default:
                    handleStatusError(response, iun, taxId);
            }
        } else {
            handleStatusError(response, iun, taxId);
        }

    }

    private void handleStatusError(ExtChannelResponse response, String iun, String taxId) {
        log.error("Specified response {} is not possibile  - iun {} id {}", response.getResponseStatus(), iun, taxId);
        throw new PnInternalException("Specified response " + response.getResponseStatus() + " is not possibile");
    }

}
