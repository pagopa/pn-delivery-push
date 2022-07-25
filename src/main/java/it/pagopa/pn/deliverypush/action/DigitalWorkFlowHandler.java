package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.DigitalMessageReferenceInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelProgressEventCat;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleDigitalWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class DigitalWorkFlowHandler {
    public static final int MAX_ATTEMPT_NUMBER = 2;

    private final ExternalChannelService externalChannelService;
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final CompletionWorkFlowHandler completionWorkflow;
    private final PublicRegistryService publicRegistryService;
    private final InstantNowSupplier instantNowSupplier;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public DigitalWorkFlowHandler(ExternalChannelService externalChannelService,
                                  NotificationService notificationService,
                                  SchedulerService schedulerService,
                                  DigitalWorkFlowUtils digitalWorkFlowUtils,
                                  CompletionWorkFlowHandler completionWorkflow,
                                  PublicRegistryService publicRegistryService,
                                  InstantNowSupplier instantNowSupplier,
                                  PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.externalChannelService = externalChannelService;
        this.notificationService = notificationService;
        this.schedulerService = schedulerService;
        this.digitalWorkFlowUtils = digitalWorkFlowUtils;
        this.completionWorkflow = completionWorkflow;
        this.publicRegistryService = publicRegistryService;
        this.instantNowSupplier = instantNowSupplier;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    public void startScheduledNextWorkflow(String iun, Integer recIndex) {
        ScheduleDigitalWorkflowDetailsInt scheduleDigitalWorkflow = digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(iun, recIndex);
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        DigitalAddressInfo digitalAddressInfo = getDigitalAddressInfo(scheduleDigitalWorkflow);
        nextWorkFlowAction(notification, recIndex, digitalAddressInfo);
    }

    /**
     * Handle digital notification Workflow based on already made attempt
     */
    private void nextWorkFlowAction(NotificationInt notification, Integer recIndex, DigitalAddressInfo lastAttemptMade) {
        log.info("Start Next Digital workflow action - iun={} id={}", notification.getIun(), recIndex);
        
        String iun = notification.getIun();
        
        //Viene ottenuta la source del prossimo indirizzo da testare, con il numero di tentativi già effettuati per tale sorgente e la data dell'ultimo tentativo
        DigitalAddressInfo nextAddressInfo = digitalWorkFlowUtils.getNextAddressInfo(iun, recIndex, lastAttemptMade);
        log.debug("Next address source is={} and attempt number already made is={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), nextAddressInfo.getSentAttemptMade(), iun, recIndex);

        if (nextAddressInfo.getSentAttemptMade() < MAX_ATTEMPT_NUMBER) {
            switch (nextAddressInfo.getSentAttemptMade()) {
                case 0:
                    log.info("Start first attempt for source={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), iun, recIndex);
                    checkAndSendNotification(notification, recIndex, nextAddressInfo);
                    break;
                case 1:
                    log.info("Start second attempt for source={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), iun, recIndex);
                    startNextWorkflow7daysAfterLastAttempt(notification, recIndex, nextAddressInfo, lastAttemptMade);
                    break;
                default:
                    log.error("Specified attempt={} is not possibile  - iun={} id={}", nextAddressInfo.getSentAttemptMade(), iun, recIndex);
                    throw new PnInternalException("Specified attempt " + nextAddressInfo.getSentAttemptMade() + " is not possibile");
            }
        } else {
            //Sono stati già effettuati tutti i tentativi possibili, la notificazione è quindi fallita
            log.info("All attempts were unsuccessful. Digital workflow is failed.  - iun={} id={}", iun, recIndex);
            completionWorkflow.completionDigitalWorkflow(notification, recIndex, instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
        }
    }

    private void checkAndSendNotification(NotificationInt notification, Integer recIndex, DigitalAddressInfo nextAddressInfo) {
        log.debug("CheckAndSendNotification  - iun={} id={}", notification.getIun(), recIndex);
        
        String iun = notification.getIun();
        log.debug("Get notification and recipient completed - iun={} id={}", iun, recIndex);

        if (DigitalAddressSourceInt.GENERAL.equals(nextAddressInfo.getDigitalAddressSource())) {
            log.debug("Address is general - iun={} id={}", iun, recIndex);
            publicRegistryService.sendRequestForGetDigitalGeneralAddress(notification, recIndex, ContactPhaseInt.SEND_ATTEMPT, nextAddressInfo.getSentAttemptMade());//general address need async call to get it
        } else {
            log.debug("Address source is not general - iun={} id={}", iun, recIndex);

            //Viene ottenuto l'indirizzo a partire dalla source
            LegalDigitalAddressInt destinationAddress = digitalWorkFlowUtils.getAddressFromSource(nextAddressInfo.getDigitalAddressSource(), recIndex, notification);
            nextAddressInfo = nextAddressInfo.toBuilder().digitalAddress(destinationAddress).build();
            log.info("Get address completed - iun={} id={}", iun, recIndex);
            //Viene Effettuato il check dell'indirizzo e l'eventuale send
            checkAddressAndSend(notification, recIndex, nextAddressInfo);
        }
    }

    /**
     * If for this address source 7 days has already passed since the last made attempt, for example because have already performed scheduling for previously
     * tried address, the notification step is called, else it is scheduled.
     */
    private void startNextWorkflow7daysAfterLastAttempt(NotificationInt notification, Integer recIndex, DigitalAddressInfo nextAddressInfo, DigitalAddressInfo lastAttemptMade) {
        log.info("StartNextWorkflow7daysAfterLastAttempt - iun={} id={}", notification.getIun(), recIndex);
        
        String iun = notification.getIun();
        Instant lastAttemptDate = nextAddressInfo.getLastAttemptDate();
        
        Instant schedulingDate = lastAttemptDate.plus(pnDeliveryPushConfigs.getTimeParams().getSecondNotificationWorkflowWaitingTime());
        //Vengono aggiunti 7 giorni alla data dell'ultimo tentativo effettuata per questa source
        if (instantNowSupplier.get().isAfter(schedulingDate)) {
            log.info("Next workflow scheduling date={} is passed. Start next workflow - iun={} id={}", schedulingDate, iun, recIndex);
            //Se la data odierna è successiva alla data ottenuta in precedenza, non c'è necessità di schedulare, perchè i 7 giorni necessari di attesa dopo il primo tentativo risultano essere già passati
            checkAndSendNotification(notification, recIndex, nextAddressInfo);
        } else {
            log.info("Next workflow scheduling date={} is not passed. Need to schedule next workflow - iun={} id={}", schedulingDate, iun, recIndex);
            //Se la data è minore alla data odierna, bisogna attendere il completamento dei 7 giorni prima partire con un nuovo workflow per questa source
            digitalWorkFlowUtils.addScheduledDigitalWorkflowToTimeline(notification, recIndex, lastAttemptMade);
            schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_NEXT_ACTION);
        }
    }

    /**
     * Handle response to request for get special address. If address is present in response, send notification to this address else startNewWorkflow action.
     *
     * @param response Get special address response
     * @param notification      Notification
     * @param prCallDetails     Public registry call details
     */
    public void handleGeneralAddressResponse(PublicRegistryResponse response, NotificationInt notification, PublicRegistryCallDetailsInt prCallDetails) {
        Integer recIndex = prCallDetails.getRecIndex();
        log.info("HandleGeneralAddressResponse - iun={} id={}", notification.getIun(), recIndex);
        
        DigitalAddressInfo lastAttemptAddressInfo = DigitalAddressInfo.builder()
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(response.getDigitalAddress())
                .sentAttemptMade(prCallDetails.getSentAttemptMade())
                .lastAttemptDate(prCallDetails.getSendDate())
                .build();

        checkAddressAndSend(notification, recIndex, lastAttemptAddressInfo);
    }

    private void checkAddressAndSend(NotificationInt notification, Integer recIndex, DigitalAddressInfo addressInfo) {
        String iun = notification.getIun();

        LegalDigitalAddressInt digitalAddress = addressInfo.getDigitalAddress();

        log.info("CheckAddressAndSend - iun={} id={}", iun, recIndex);

        if (digitalAddress != null) {
            log.info("Address is available, send notification to external channel - iun={} id={}", iun, recIndex);

            //Se l'indirizzo è disponibile, dunque valorizzato viene inviata la notifica a external channel ...
            digitalWorkFlowUtils.addAvailabilitySourceToTimeline(recIndex, notification, addressInfo.getDigitalAddressSource(), true, addressInfo.getSentAttemptMade());
            externalChannelService.sendDigitalNotification(notification, digitalAddress, addressInfo.getDigitalAddressSource(), recIndex, addressInfo.getSentAttemptMade());
        } else {
            //... altrimenti si passa alla prossima workflow action
            log.info("Address is not available, need to start next workflow action - iun={} id={}", iun, recIndex);
            digitalWorkFlowUtils.addAvailabilitySourceToTimeline(recIndex, notification, addressInfo.getDigitalAddressSource(), false, addressInfo.getSentAttemptMade());

            nextWorkFlowAction(notification, recIndex, addressInfo);
        }
    }

    public void handleExternalChannelResponse(ExtChannelDigitalSentResponseInt response) {
        String iun = response.getIun();
        log.info("HandleExternalChannelResponse - iun={} requestId={}", iun, response.getRequestId());
        
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        
        TimelineElementInternal sendDigitalTimelineElement = 
                digitalWorkFlowUtils.getSendDigitalDetailsTimelineElement(iun, response.getRequestId());

        SendDigitalDetailsInt sendDigitalDetails = (SendDigitalDetailsInt) sendDigitalTimelineElement.getDetails();
        
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        Integer recIndex = sendDigitalDetails.getRecIndex();

        ResponseStatusInt status = mapDigitalStatusInResponseStatus(response.getStatus());
        
        if (status != null) {

            PnAuditLogEvent logEvent = auditLogBuilder
                    .before(PnAuditLogEventType.AUD_NT_CHECK, "Digital workflow Ext channel response for source {} and retryNumber={} - iun={} id={}",
                            sendDigitalDetails.getDigitalAddressSource(), sendDigitalDetails.getRetryNumber(), iun, recIndex)
                    .iun(iun)
                    .build();
            logEvent.log();
            
            digitalWorkFlowUtils.addDigitalFeedbackTimelineElement(notification, status, response.getEventDetails()==null?null:List.of(response.getEventDetails()), 
                    sendDigitalDetails, response.getDigitalMessageReferenceInt());

            switch (status) {
                case OK:
                    logEvent.generateSuccess().log();

                    log.info("Notification sent successfully, starting completion workflow - iun={} id={}", iun, recIndex);
                    //La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                    completionWorkflow.completionDigitalWorkflow(notification, recIndex, response.getEventTimestamp(), sendDigitalDetails.getDigitalAddress(), EndWorkflowStatus.SUCCESS);
                    break;
                case KO:
                    //Non è stato possibile effettuare la notificazione, si passa al prossimo step del workflow
                    log.info("Notification failed, starting next workflow action - iun={} id={}", iun, recIndex);
                    
                    logEvent.generateFailure("Notification failed for eventCode={} eventDetails={}",
                            response.getEventCode(), response.getEventDetails()).log();

                    DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                            .digitalAddressSource(sendDigitalDetails.getDigitalAddressSource())
                            .lastAttemptDate(sendDigitalTimelineElement.getTimestamp())
                            .build();

                    nextWorkFlowAction(notification, recIndex, lastAttemptMade);
                    break;
            }
        } else {
            handleStatusProgress(response.getStatus(), notification, recIndex, sendDigitalDetails, response.getDigitalMessageReferenceInt());
        }
    }

    private void handleStatusProgress( ExtChannelProgressEventCat status,
                                      NotificationInt notification,
                                      Integer recIndex,
                                      SendDigitalDetailsInt sendDigitalDetails,
                                      DigitalMessageReferenceInt digitalMessageReference ) {
        log.info("Specified status={} is not final - iun={} id={}", status, notification.getIun(), recIndex);
        
        int index = digitalWorkFlowUtils.getDigitalDeliveringProgressTimelineElementIndex(notification, recIndex, sendDigitalDetails.getDigitalAddressSource(), sendDigitalDetails.getRetryNumber());
        log.debug("Index for new timeline progress is {} - iun={} id={}", index, notification.getIun(), recIndex);

        digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(notification, sendDigitalDetails, Collections.singletonList(digitalMessageReference), index);
    }

    private DigitalAddressInfo getDigitalAddressInfo(ScheduleDigitalWorkflowDetailsInt scheduleDigitalWorkflow) {
        return DigitalAddressInfo.builder()
                .digitalAddress(scheduleDigitalWorkflow.getDigitalAddress())
                .digitalAddressSource(scheduleDigitalWorkflow.getDigitalAddressSource())
                .lastAttemptDate(scheduleDigitalWorkflow.getLastAttemptDate())
                .sentAttemptMade(scheduleDigitalWorkflow.getSentAttemptMade())
                .build();
    }

    private ResponseStatusInt mapDigitalStatusInResponseStatus(ExtChannelProgressEventCat digitalStatus)
    {
        /* si traduce l'enum
            [ PROGRESS, OK, RETRIABLE_ERROR, ERROR ]
        */
        switch (digitalStatus)
        {
            case PROGRESS:
                return null;
            case OK:
                return ResponseStatusInt.OK;
            case ERROR:
            case RETRIABLE_ERROR:
                return ResponseStatusInt.KO;
            default:
                throw new PnInternalException("Invalid digitalStatus received: " + digitalStatus);
        }
    }
    
}
