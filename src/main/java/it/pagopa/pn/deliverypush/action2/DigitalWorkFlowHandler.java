package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.AttemptAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class DigitalWorkFlowHandler {
    public static final int MAX_ATTEMPT_NUMBER = 2;

    private final CompletionWorkFlowHandler completionWorkFlow;
    private final ExternalChannelService externalChannelService;
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final DigitaWorkFlowService digitalService;
    private final CompletionWorkFlowHandler completionWorkflow;
    private final TimelineService timelineService;
    private final PublicRegistryService publicRegistryService;

    public DigitalWorkFlowHandler(CompletionWorkFlowHandler completionWorkFlow, ExternalChannelService externalChannelService, NotificationService notificationService,
                                  SchedulerService schedulerService, DigitaWorkFlowService digitalService, CompletionWorkFlowHandler completionWorkflow,
                                  TimelineService timelineService, PublicRegistryService publicRegistryService) {
        this.completionWorkFlow = completionWorkFlow;
        this.externalChannelService = externalChannelService;
        this.notificationService = notificationService;
        this.schedulerService = schedulerService;
        this.digitalService = digitalService;
        this.completionWorkflow = completionWorkflow;
        this.timelineService = timelineService;
        this.publicRegistryService = publicRegistryService;
    }

    /**
     * Handle digital notification Workflow based on already made attempt
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    public void nextWorkFlowAction(String iun, String taxId) {
        log.info("Next Digital workflow action for iun {} id {}", iun, taxId);

        //Viene ottenuta la source del prossimo indirizzo da testare, con il numero di tentativi già effettuati per tale sorgente e la data dell'ultimo tentativo
        AttemptAddressInfo nextAddressInfo = digitalService.getNextAddressInfo(iun, taxId);
        log.debug("Next address source is {} and attempt number already made is {}", nextAddressInfo.getAddressSource(), nextAddressInfo.getSentAttemptMade());

        if (nextAddressInfo.getSentAttemptMade() < MAX_ATTEMPT_NUMBER) {
            switch (nextAddressInfo.getSentAttemptMade()) {
                case 0:
                    log.info("Start first attempt for source {}", nextAddressInfo.getAddressSource());
                    checkAndSendNotification(iun, taxId, nextAddressInfo, nextAddressInfo.getSentAttemptMade());
                    break;
                case 1:
                    log.info("Start second attempt for source {}", nextAddressInfo.getAddressSource());
                    startNextWorkflow7daysAfterLastAttempt(iun, taxId, nextAddressInfo, nextAddressInfo.getSentAttemptMade());
                    break;
                default:
                    log.error("Is not possibile to have {} number of attempt. Iun {} id {}", nextAddressInfo.getSentAttemptMade(), iun, taxId);
                    throw new PnInternalException("Is not possibile to have " + nextAddressInfo.getSentAttemptMade() + ". Iun " + iun + " id " + taxId);
            }
        } else {
            //Sono stati già effettuati tutti i tentativi possibili, la notificazione è quindi fallita
            log.info("Digital workflow is failed because all planned attempt have failed for iun {} id {}", iun, taxId);
            completionWorkFlow.completionDigitalWorkflow(taxId, iun, Instant.now(), null, EndWorkflowStatus.FAILURE);
        }
    }

    private void checkAndSendNotification(String iun, String taxId, AttemptAddressInfo nextAddressInfo, int sentAttemptMade) {

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);
        log.debug("Get notification and recipient completed ");

        if (DigitalAddressSource2.GENERAL.equals(nextAddressInfo.getAddressSource())) {
            publicRegistryService.sendRequestForGetAddress(iun, taxId, DeliveryMode.DIGITAL, ContactPhase.SEND_ATTEMPT, sentAttemptMade);//general address need async call to get it
        } else {
            sendNotificationOrStartNextWorkflowAction(nextAddressInfo.getAddressSource(), recipient, notification, sentAttemptMade);
        }
    }

    /**
     * If for this address source 7 days has already passed since the last made attempt, for example because have already performed scheduling for previously
     * tried address, the notification step is called, else it is scheduled.
     *
     * @param iun             Notification unique identifier
     * @param taxId           User identifier
     * @param nextAddressInfo Next Address source information
     */
    private void startNextWorkflow7daysAfterLastAttempt(String iun, String taxId, AttemptAddressInfo nextAddressInfo, int sentAttemptMade) {
        Instant schedulingDate = nextAddressInfo.getLastAttemptDate().plus(7, ChronoUnit.DAYS);
        //Vengono aggiunti 7 giorni alla data dell'ultimo tentativo effettuata per questa source

        if (Instant.now().isAfter(schedulingDate)) {
            log.debug("Next workflow scheduling date {} is passed. Start next workflow ", schedulingDate);
            //Se la data odierna è successiva alla data ottenuta in precedenza, non c'è necessità di schedulare, perchè i 7 giorni necessari di attesa dopo il primo tentativo risultano essere già passati
            checkAndSendNotification(iun, taxId, nextAddressInfo, sentAttemptMade);
        } else {
            log.debug("Next workflow scheduling date {} is not passed. Need to schedule next workflow ", schedulingDate);
            //Se la data è minore alla data odierna, bisogna attendere il completamento dei 7 giorni prima partire con un nuovo workflow per questa source
            schedulerService.schedulEvent(iun, taxId, schedulingDate, ActionType.DIGITAL_WORKFLOW_NEXT_ACTION);
        }
    }

    private void sendNotificationOrStartNextWorkflowAction(DigitalAddressSource2 addressSource, NotificationRecipient recipient, Notification notification, int sentAttemptMade) {
        log.debug("Start sendNotificationOrStartNextWorkflowAction for addressSource {}", addressSource);

        //Viene ottenuto l'indirizzo a partire dalla source
        DigitalAddress destinationAddress = digitalService.getAddressFromSource(addressSource, recipient, notification);
        //Viene Effettuato il check dell'indirizzo e l'eventuale send
        checkAddressAndSend(recipient, notification, destinationAddress, addressSource, sentAttemptMade);
    }

    /**
     * Handle response to request for get special address. If address is present in response, send notification to this address else startNewWorkflow action.
     *
     * @param response Get special address response
     * @param iun      Notification unique identifier
     * @param taxId    User identifier
     */
    public void handleGeneralAddressResponse(PublicRegistryResponse response, String iun, String taxId, int sentAttemptMade) {

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);

        log.debug("Received general address response, get notification and recipient completed");
        checkAddressAndSend(recipient, notification, response.getDigitalAddress(), DigitalAddressSource2.GENERAL, sentAttemptMade);
    }

    private void checkAddressAndSend(NotificationRecipient recipient, Notification notification, DigitalAddress address, DigitalAddressSource2 addressSource, int sentAttemptMade) {
        String iun = notification.getIun();
        String taxId = recipient.getTaxId();

        if (address != null) {
            log.debug("Address is available, send notification to external channel");

            //Se l'indirizzo è disponibile, dunque valorizzato viene inviata la notifica ad external channel ...
            timelineService.addAvailabilitySourceToTimeline(taxId, iun, addressSource, true, sentAttemptMade);
            externalChannelService.sendDigitalNotification(notification, address, addressSource, recipient, sentAttemptMade);

        } else {
            //... altrimenti si passa alla prossima workflow action
            log.debug("Address is not available, need to start next workflow action ");

            timelineService.addAvailabilitySourceToTimeline(taxId, iun, addressSource, false, sentAttemptMade);
            nextWorkFlowAction(iun, taxId);
        }
    }

    public void handleExternalChannelResponse(ExtChannelResponse response) {
        //Conservare ricevuta PEC //TODO capire cosa si intende

        switch (response.getResponseStatus()) {
            case OK:
                log.info("Notification sent successfully, starting completion workflow ");
                //La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                completionWorkflow.completionDigitalWorkflow(response.getTaxId(), response.getIun(), response.getNotificationDate(), response.getDigitalUsedAddress(), EndWorkflowStatus.SUCCESS);
                break;
            case KO:
                //Non è stato possibile effettuare la notificazione, si passa al prossimo step del workflow
                timelineService.addDigitalFailureAttemptToTimeline(response);
                log.info("Notificazione failed, starting next workflow action ");
                nextWorkFlowAction(response.getIun(), response.getTaxId());
                break;
        }
    }

}
