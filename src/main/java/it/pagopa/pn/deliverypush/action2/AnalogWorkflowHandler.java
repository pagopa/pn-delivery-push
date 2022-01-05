package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action2.utils.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action2.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class AnalogWorkflowHandler {
    private final NotificationService notificationService;
    private final ExternalChannelUtils externalChannelUtils;
    private final CompletionWorkFlowHandler completionWorkFlow;
    private final AnalogWorkflowUtils analogWorkflowUtils;
    private final PublicRegistryUtils publicRegistryUtils;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public AnalogWorkflowHandler(NotificationService notificationService, ExternalChannelUtils externalChannelUtils,
                                 CompletionWorkFlowHandler completionWorkFlow, AnalogWorkflowUtils analogWorkflowUtils,
                                 PublicRegistryUtils publicRegistryUtils, TimelineService timeLineService,
                                 TimelineUtils timelineUtils) {
        this.notificationService = notificationService;
        this.externalChannelUtils = externalChannelUtils;
        this.completionWorkFlow = completionWorkFlow;
        this.analogWorkflowUtils = analogWorkflowUtils;
        this.publicRegistryUtils = publicRegistryUtils;
        this.timelineService = timeLineService;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Handle analog notification Workflow based on already made attempt
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    @StreamListener(condition = "ANALOG_WORKFLOW")
    public void nextWorkflowStep(String iun, String taxId) {
        log.info("Start analog next workflow action for iun {} id {}", iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);
        log.debug("Get notification and recipient completed");

        int sentAttemptMade = analogWorkflowUtils.getSentAttemptFromTimeLine(iun, taxId);
        log.debug("Sent attempt made is {}", sentAttemptMade);

        switch (sentAttemptMade) {
            case 0:
                log.info("Handle send first attempt");

                PhysicalAddress paProvidedAddress = recipient.getPhysicalAddress();

                if (paProvidedAddress != null) {
                    log.info("Start send notification with Pa address");
                    //send notification with paAddress
                    externalChannelUtils.sendAnalogNotification(notification, paProvidedAddress, recipient, true, sentAttemptMade);
                } else {
                    log.info("Pa address is not available, need to get address from public registry");
                    //Get address for notification from public registry
                    publicRegistryUtils.sendRequestForGetPhysicalAddress(iun, taxId, sentAttemptMade);
                }
                break;
            case 1:
                log.info("Handle second attempt");
                //An send attempt was already made, get address from public registry for second send attempt
                publicRegistryUtils.sendRequestForGetPhysicalAddress(iun, taxId, sentAttemptMade);
                break;
            case 2:
                // All sent attempts have been made. The user is not reachable
                log.info("User with iun {} and id {} is unreachable, all attempt was failed", iun, taxId);
                completionWorkFlow.completionAnalogWorkflow(taxId, iun, Instant.now(), null, EndWorkflowStatus.FAILURE);
                break;
            default:
                log.error("Specified attempt {} is not possibile", sentAttemptMade);
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
    public void handlePublicRegistryResponse(String iun, String taxId, PublicRegistryResponse response) {
        log.info("Start analog next workflow action for iun {} id {}", iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);

        int sentAttemptMade = analogWorkflowUtils.getSentAttemptFromTimeLine(iun, taxId);
        log.info("sentAttemptMade is {}", sentAttemptMade);

        switch (sentAttemptMade) {
            case 0:
                log.info("Handle public registry response for first attempt");
                checkAddressAndSend(notification, recipient, response.getPhysicalAddress(), true, sentAttemptMade);
                break;
            case 1:
                log.info("Handle public registry response for second attempt");
                //Ottenuta risposta alla seconda send
                publicRegistrySecondSendResponse(response, notification, recipient, sentAttemptMade);
                break;
            default:
                log.error("Specified attempt {} is not possibile", sentAttemptMade);
                throw new PnInternalException("Specified attempt " + sentAttemptMade + " is not possibile");
        }
    }

    private void publicRegistrySecondSendResponse(PublicRegistryResponse response, Notification notification, NotificationRecipient recipient, int sentAttemptMade) {
        String iun = notification.getIun();
        String taxId = recipient.getTaxId();
        log.info("Start publicRegistrySecondSendResponse for iun {} id {}", iun, taxId);

        //Vengono ottenute le informazioni del primo invio effettuato tramite external channel dalla timeline
        SendPaperFeedbackDetails lastSentFeedback = analogWorkflowUtils.getLastTimelineSentFeedback(iun, taxId);
        log.debug("getLastTimelineSentFeedback completed for iun {} id {}", iun, taxId);

        //Se l'indirizzo fornito da public registry è presente ...
        if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {

            PhysicalAddress lastUsedAddress = lastSentFeedback.getAddress();

            //... e risulta diverso da quello utilizzato nel primo tentativo, viene inviata seconda notifica ad external channel con questo indirizzo
            if (!response.getPhysicalAddress().equals(lastUsedAddress)) { //TODO Da definire in maniera chiara il metodo equals
                log.info("Send second notification to external channel with public registry response address for iun {} id {}", iun, taxId);
                externalChannelUtils.sendAnalogNotification(notification, response.getPhysicalAddress(), recipient, false, sentAttemptMade);
            } else {
                log.info("First send address and public registry response address are equals for iun {} id {}", iun, taxId);
                //... se i due indirizzi sono uguali, viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
                checkAddressAndSend(notification, recipient, lastSentFeedback.getNewAddress(), false, sentAttemptMade);
            }
        } else {
            log.info("Public registry response address is empty for iun {} id {}", iun, taxId);
            //Viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
            checkAddressAndSend(notification, recipient, lastSentFeedback.getNewAddress(), false, sentAttemptMade);
        }
    }

    /**
     * If during last failed sent notification a new address has been obtained send notification else the user is unreachable
     */
    private void checkAddressAndSend(Notification notification, NotificationRecipient recipient, PhysicalAddress address, boolean investigation, int sentAttemptMade) {
        //Se l'indirizzo passato è valorizzato viene inviata la notifica ad external channel...
        if (address != null && address.getAddress() != null) {
            log.info("Have a valid address, send notification to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());
            externalChannelUtils.sendAnalogNotification(notification, address, recipient, investigation, sentAttemptMade);
        } else {
            //... se l'indirizzo non è presente non è possibile raggiungere il destinario che risulta irreperibile 
            log.info("Address isn't valid, user is unreachable for iun {} id {}", notification.getIun(), recipient.getTaxId());
            completionWorkFlow.completionAnalogWorkflow(recipient.getTaxId(), notification.getIun(), Instant.now(), null, EndWorkflowStatus.FAILURE);
        }
    }

    public void extChannelResponseHandler(ExtChannelResponse response) {
        log.info("Get response from external channel for iun {} id {} with status {}", response.getIun(), response.getTaxId(), response.getResponseStatus());

        switch (response.getResponseStatus()) {
            case OK:
                // La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                completionWorkFlow.completionAnalogWorkflow(response.getTaxId(), response.getIun(), response.getNotificationDate(), response.getAnalogUsedAddress(), EndWorkflowStatus.SUCCESS);
                break;
            case KO:
                // External channel non è riuscito ad effettuare la notificazione, si passa al prossimo step del workflow
                int sentAttemptMade = analogWorkflowUtils.getSentAttemptFromTimeLine(response.getIun(), response.getTaxId()); //TODO Valutare di cambiare tale logica e ricevere da extchannel il numero di tentativi effettuati
                addTimelineElement(timelineUtils.buildAnalogFailureAttemptTimelineElement(response, sentAttemptMade));
                nextWorkflowStep(response.getIun(), response.getTaxId());
                break;
            default:
                log.error("Specified response {} is not possibile", response.getResponseStatus());
                throw new PnInternalException("Specified response " + response.getResponseStatus() + " is not possibile");
        }
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }
}
