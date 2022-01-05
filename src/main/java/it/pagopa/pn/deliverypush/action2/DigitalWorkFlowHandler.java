package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.AttemptAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action2.utils.PublicRegistryUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class DigitalWorkFlowHandler {
    public static final int MAX_ATTEMPT_NUMBER = 2;
    public static final int SECOND_NOTIFICATION_WORKFLOW_WAITING_TIME = 7;

    private final ExternalChannelUtils externalChannelUtils;
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final CompletionWorkFlowHandler completionWorkflow;
    private final TimelineService timelineService;
    private final PublicRegistryUtils publicRegistryUtils;
    private final TimelineUtils timelineUtils;

    public DigitalWorkFlowHandler(ExternalChannelUtils externalChannelUtils,
                                  NotificationService notificationService, SchedulerService schedulerService,
                                  DigitalWorkFlowUtils digitalWorkFlowUtils, CompletionWorkFlowHandler completionWorkflow,
                                  TimelineService timelineService, PublicRegistryUtils publicRegistryUtils,
                                  TimelineUtils timelineUtils) {
        this.externalChannelUtils = externalChannelUtils;
        this.notificationService = notificationService;
        this.schedulerService = schedulerService;
        this.digitalWorkFlowUtils = digitalWorkFlowUtils;
        this.completionWorkflow = completionWorkflow;
        this.timelineService = timelineService;
        this.publicRegistryUtils = publicRegistryUtils;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Handle digital notification Workflow based on already made attempt
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    @StreamListener(condition = "DIGITAL_WORKFLOW")
    public void nextWorkFlowAction(String iun, String taxId) {
        log.info("Next Digital workflow action for iun {} id {}", iun, taxId);

        //Viene ottenuta la source del prossimo indirizzo da testare, con il numero di tentativi già effettuati per tale sorgente e la data dell'ultimo tentativo
        AttemptAddressInfo nextAddressInfo = digitalWorkFlowUtils.getNextAddressInfo(iun, taxId);
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
                    log.error("Specified attempt {} is not possibile", nextAddressInfo.getSentAttemptMade());
                    throw new PnInternalException("Specified attempt " + nextAddressInfo.getSentAttemptMade() + " is not possibile");
            }
        } else {
            //Sono stati già effettuati tutti i tentativi possibili, la notificazione è quindi fallita
            log.info("Digital workflow is failed because all planned attempt have failed for iun {} id {}", iun, taxId);
            completionWorkflow.completionDigitalWorkflow(taxId, iun, Instant.now(), null, EndWorkflowStatus.FAILURE);
        }
    }

    private void checkAndSendNotification(String iun, String taxId, AttemptAddressInfo nextAddressInfo, int sentAttemptMade) {
        log.info("Start checkAndSendNotification for iun {} id {}", iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);
        log.debug("Get notification and recipient completed ");

        if (DigitalAddressSource.GENERAL.equals(nextAddressInfo.getAddressSource())) {
            log.debug("Address is general");
            publicRegistryUtils.sendRequestForGetDigitalAddress(iun, taxId, ContactPhase.SEND_ATTEMPT, sentAttemptMade);//general address need async call to get it

        } else {
            log.debug("Address source is not general");

            //Viene ottenuto l'indirizzo a partire dalla source
            DigitalAddress destinationAddress = digitalWorkFlowUtils.getAddressFromSource(nextAddressInfo.getAddressSource(), recipient, notification);
            log.info("Get address completed");
            //Viene Effettuato il check dell'indirizzo e l'eventuale send
            checkAddressAndSend(recipient, notification, destinationAddress, nextAddressInfo.getAddressSource(), sentAttemptMade);
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
        log.info("Start startNextWorkflow7daysAfterLastAttempt for iun {} id {}", iun, taxId);

        Instant schedulingDate = nextAddressInfo.getLastAttemptDate().plus(SECOND_NOTIFICATION_WORKFLOW_WAITING_TIME, ChronoUnit.DAYS);
        //Vengono aggiunti 7 giorni alla data dell'ultimo tentativo effettuata per questa source

        if (Instant.now().isAfter(schedulingDate)) {
            log.info("Next workflow scheduling date {} is passed. Start next workflow ", schedulingDate);
            //Se la data odierna è successiva alla data ottenuta in precedenza, non c'è necessità di schedulare, perchè i 7 giorni necessari di attesa dopo il primo tentativo risultano essere già passati
            checkAndSendNotification(iun, taxId, nextAddressInfo, sentAttemptMade);
        } else {
            log.info("Next workflow scheduling date {} is not passed. Need to schedule next workflow ", schedulingDate);
            //Se la data è minore alla data odierna, bisogna attendere il completamento dei 7 giorni prima partire con un nuovo workflow per questa source
            schedulerService.scheduleEvent(iun, taxId, schedulingDate, ActionType.DIGITAL_WORKFLOW_NEXT_ACTION);
        }
    }

    /**
     * Handle response to request for get special address. If address is present in response, send notification to this address else startNewWorkflow action.
     *
     * @param response Get special address response
     * @param iun      Notification unique identifier
     * @param taxId    User identifier
     */
    public void handleGeneralAddressResponse(PublicRegistryResponse response, String iun, String taxId, int sentAttemptMade) {
        log.info("Start handleGeneralAddressResponse for iun {} id {}", iun, taxId);

        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);

        log.debug("Received general address response, get notification and recipient completed");
        checkAddressAndSend(recipient, notification, response.getDigitalAddress(), DigitalAddressSource.GENERAL, sentAttemptMade);
    }

    private void checkAddressAndSend(NotificationRecipient recipient, Notification notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, int sentAttemptMade) {
        String iun = notification.getIun();
        String taxId = recipient.getTaxId();
        log.info("Start checkAddressAndSend for iun {} id {}", iun, taxId);

        if (digitalAddress != null && digitalAddress.getAddress() != null) {
            log.info("Address is available, send notification to external channel");

            //Se l'indirizzo è disponibile, dunque valorizzato viene inviata la notifica ad external channel ...
            addTimelineElement(timelineUtils.buildAvailabilitySourceTimelineElement(taxId, iun, addressSource, true, sentAttemptMade));
            externalChannelUtils.sendDigitalNotification(notification, digitalAddress, addressSource, recipient, sentAttemptMade);

        } else {
            //... altrimenti si passa alla prossima workflow action
            log.info("Address is not available, need to start next workflow action ");

            addTimelineElement(timelineUtils.buildAvailabilitySourceTimelineElement(taxId, iun, addressSource, false, sentAttemptMade));
            nextWorkFlowAction(iun, taxId);
        }
    }

    public void handleExternalChannelResponse(ExtChannelResponse response) {
        //Conservare ricevuta PEC //TODO capire cosa si intende
        log.info("Start handleExternalChannelResponse for iun {} id {}", response.getIun(), response.getTaxId());

        switch (response.getResponseStatus()) {
            case OK:
                log.info("Notification sent successfully, starting completion workflow");
                //La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                completionWorkflow.completionDigitalWorkflow(response.getTaxId(), response.getIun(), response.getNotificationDate(), response.getDigitalUsedAddress(), EndWorkflowStatus.SUCCESS);
                break;
            case KO:
                //Non è stato possibile effettuare la notificazione, si passa al prossimo step del workflow
                addTimelineElement(timelineUtils.buildDigitalFailureAttemptTimelineElement(response));
                log.info("Notificazione failed, starting next workflow action");
                nextWorkFlowAction(response.getIun(), response.getTaxId());
                break;
            default:
                log.error("Specified status {} is not possibile", response.getResponseStatus());
                throw new PnInternalException("Specified status" + response.getResponseStatus() + " is not possibile");
        }
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }

}
