package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class AnalogWorkflowHandler {
    private final NotificationService notificationService;
    private final ExternalChannelService externalChannelService;
    private final CompletionWorkFlowHandler completionWorkFlow;
    private final SchedulerService schedulerService;
    private final AnalogWorkflowService analogService;
    private final PublicRegistryService publicRegistryService;
    private final TimelineService timeLineService;

    public AnalogWorkflowHandler(NotificationService notificationService, ExternalChannelService externalChannelService, CompletionWorkFlowHandler completionWorkFlow,
                                 SchedulerService schedulerService, AnalogWorkflowService analogService, PublicRegistryService publicRegistryService,
                                 TimelineService timeLineService) {
        this.notificationService = notificationService;
        this.externalChannelService = externalChannelService;
        this.completionWorkFlow = completionWorkFlow;
        this.schedulerService = schedulerService;
        this.analogService = analogService;
        this.publicRegistryService = publicRegistryService;
        this.timeLineService = timeLineService;
    }

    /**
     * Handle analog notification Workflow based on already made attempt
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    public void nextWorkflowStep(String iun, String taxId) {
        log.info("Start analog next workflow action for iun {} id {}", iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);
        log.debug("Get notification and recipient completed");

        int sentAttemptMade = analogService.getSentAttemptFromTimeLine(iun, taxId);
        log.debug("Sent attempt made is {}", sentAttemptMade);

        switch (sentAttemptMade) {
            case 0:
                log.info("Handle send first attempt");

                PhysicalAddress paProvidedAddress = recipient.getPhysicalAddress();

                if (paProvidedAddress != null) {
                    log.info("Start send notification with Pa address");
                    //send notification with paAddress
                    externalChannelService.sendAnalogNotification(notification, paProvidedAddress, recipient, true, sentAttemptMade);
                } else {
                    log.info("Pa address is not available, need to get address from public registry");
                    //Get address for notification from public registry
                    publicRegistryService.sendRequestForGetAddress(iun, taxId, DeliveryMode.ANALOG, ContactPhase.SEND_ATTEMPT, sentAttemptMade);
                }
                break;
            case 1:
                log.info("Handle second attempt");
                //An send attempt was already made, get address from public registry for second send attempt
                publicRegistryService.sendRequestForGetAddress(iun, taxId, DeliveryMode.ANALOG, ContactPhase.SEND_ATTEMPT, sentAttemptMade);
                break;
            case 2:
                // All sent attempts have been made. The user is not reachable
                unreachableUser(iun, taxId);
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
        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);

        int sentAttemptMade = analogService.getSentAttemptFromTimeLine(iun, taxId);

        switch (sentAttemptMade) {
            case 0:
                checkAddressAndSend(notification, recipient, response.getPhysicalAddress(), true, sentAttemptMade);
                break;
            case 1:
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

        //Get external channel last feedback information returned from timeline
        SendPaperFeedbackDetails lastSentFeedback = analogService.getLastTimelineSentFeedback(iun, taxId);

        if (response.getPhysicalAddress() != null && response.getPhysicalAddress().getAddress() != null) {
            PhysicalAddress lastUsedAddress = lastSentFeedback.getAddress();

            //check if response address is different to last used address to avoid resending failure notification to the same address
            if (!response.getPhysicalAddress().equals(lastUsedAddress)) { //TODO Da definire in maniera chiara il metodo equals
                externalChannelService.sendAnalogNotification(notification, response.getPhysicalAddress(), recipient, false, sentAttemptMade);
            } else {
                //Send notification with investigation address if it is available
                checkAddressAndSend(notification, recipient, lastSentFeedback.getNewAddress(), false, sentAttemptMade);
            }
        } else {
            //Send notification with investigation address if it is available
            checkAddressAndSend(notification, recipient, lastSentFeedback.getNewAddress(), false, sentAttemptMade);
        }
    }

    /**
     * If during last failed sent notification a new address has been obtained send notification else the user is unreachable
     */
    private void checkAddressAndSend(Notification notification, NotificationRecipient recipient, PhysicalAddress address, boolean investigation, int sentAttemptMade) {
        if (address != null && address.getAddress() != null) {
            externalChannelService.sendAnalogNotification(notification, address, recipient, investigation, sentAttemptMade);
        } else {
            // the user is unreachable
            unreachableUser(notification.getIun(), recipient.getTaxId());
        }
    }

    public void extChannelResponseHandler(ExtChannelResponse response) {
        log.info("Get response from external channel for iun {} id {} with status {}", response.getIun(), response.getTaxId(), response.getResponseStatus());

        switch (response.getResponseStatus()) {
            case OK:
                //Se viene la notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                completionWorkFlow.completionAnalogWorkflow(response.getTaxId(), response.getIun(), response.getNotificationDate(), response.getAnalogUsedAddress(), EndWorkflowStatus.SUCCESS);
                break;
            case KO:
                //Se external channel non è riuscito ad effettuare la notificazione, si passa al prossimo step del workflow
                timeLineService.addAnalogFailureAttemptToTimeline(response, analogService.getSentAttemptFromTimeLine(response.getIun(), response.getTaxId()));
                nextWorkflowStep(response.getIun(), response.getTaxId());
                break;
        }
    }

    private void unreachableUser(String iun, String taxId) {
        log.info("User with iun {} and id {} is unreachable, all attempt was failed", iun, taxId);
        schedulerService.schedulEvent(iun, taxId, Instant.now(), ActionType.COMPLETELY_UNREACHABLE);
        completionWorkFlow.completionAnalogWorkflow(taxId, iun, Instant.now(), null, EndWorkflowStatus.FAILURE);
    }

}
