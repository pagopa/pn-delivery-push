package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDATTEMPT;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE;

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
        log.debug("startScheduledNextWorkflow - iun={} recIndex={}", iun, recIndex);

        ScheduleDigitalWorkflowDetailsInt scheduleDigitalWorkflow = digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(iun, recIndex);
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        DigitalAddressInfo digitalAddressInfo = getDigitalAddressInfo(scheduleDigitalWorkflow);
        nextWorkFlowAction(notification, recIndex, digitalAddressInfo);
    }

    public void startScheduledRetryWorkflow(String iun, Integer recIndex) {
        log.debug("startScheduledRetryWorkflow - iun={} recIndex={}", iun, recIndex);

        SendDigitalProgressDetailsInt sendDigitalProgressDetailsInt = digitalWorkFlowUtils.getMostRecentSendDigitalProgressTimelineElement(iun, recIndex);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        sendRetryNotification(notification, recIndex, DigitalAddressInfo.builder()
                .digitalAddress(sendDigitalProgressDetailsInt.getDigitalAddress())
                .digitalAddressSource(sendDigitalProgressDetailsInt.getDigitalAddressSource())
                .lastAttemptDate(sendDigitalProgressDetailsInt.getNotificationDate())
                .sentAttemptMade(sendDigitalProgressDetailsInt.getRetryNumber())
                .build());
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
                    log.info("Start check first attempt for source={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), iun, recIndex);
                    checkAndSendNotification(notification, recIndex, nextAddressInfo);
                    break;
                case 1:
                    log.info("Start second attempt for source={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), iun, recIndex);
                    startNextWorkflow7daysAfterLastAttempt(notification, recIndex, nextAddressInfo, lastAttemptMade);
                    break;
                default:
                    log.error("Specified attempt={} is not possibile  - iun={} id={}", nextAddressInfo.getSentAttemptMade(), iun, recIndex);
                    throw new PnInternalException("Specified attempt " + nextAddressInfo.getSentAttemptMade() + " is not possibile", ERROR_CODE_DELIVERYPUSH_INVALIDATTEMPT);
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
            log.info("Address is general - iun={} id={}", iun, recIndex);
            publicRegistryService.sendRequestForGetDigitalGeneralAddress(notification, recIndex, ContactPhaseInt.SEND_ATTEMPT, nextAddressInfo.getSentAttemptMade());//general address need async call to get it
        } else {
            log.debug("Address source is not general - iun={} id={}", iun, recIndex);

            //Viene ottenuto l'indirizzo a partire dalla source
            LegalDigitalAddressInt destinationAddress = digitalWorkFlowUtils.getAddressFromSource(nextAddressInfo.getDigitalAddressSource(), recIndex, notification);
            nextAddressInfo = nextAddressInfo.toBuilder().digitalAddress(destinationAddress).build();
            log.debug("Get address completed - iun={} id={}", iun, recIndex);
            //Viene Effettuato il check dell'indirizzo e l'eventuale send
            checkAddressAndSend(notification, recIndex, nextAddressInfo);
        }
    }

    private void sendRetryNotification(NotificationInt notification, Integer recIndex, DigitalAddressInfo addressInfo){
        externalChannelService.sendDigitalNotification(notification, addressInfo.getDigitalAddress(), addressInfo.getDigitalAddressSource(), recIndex, addressInfo.getSentAttemptMade(), true);
    }

    /**
     * If for this address source 7 days has already passed since the last made attempt, for example because have already performed scheduling for previously
     * tried address, the notification step is called, else it is scheduled.
     */
    private void startNextWorkflow7daysAfterLastAttempt(NotificationInt notification, Integer recIndex, DigitalAddressInfo nextAddressInfo, DigitalAddressInfo lastAttemptMade) {
        log.debug("StartNextWorkflow7daysAfterLastAttempt - iun={} id={}", notification.getIun(), recIndex);
        
        String iun = notification.getIun();
        Instant lastAttemptDate = nextAddressInfo.getLastAttemptDate();

        Duration secondNotificationWorkflowWaitingTime = pnDeliveryPushConfigs.getTimeParams().getSecondNotificationWorkflowWaitingTime();
        
        Instant schedulingDate = lastAttemptDate.plus(secondNotificationWorkflowWaitingTime);
        Instant now = instantNowSupplier.get();

        log.debug("Check scheduling nextAttempt, lastAttemptDate={} secondNotificationWorkflowWaitingTime={} schedulingDate={} now={}- iun={} id={}",
                lastAttemptDate, secondNotificationWorkflowWaitingTime, schedulingDate, now, notification.getIun(), recIndex);

        //Vengono aggiunti 7 giorni alla data dell'ultimo tentativo effettuata per questa source
        if (now.isAfter(schedulingDate)) {
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
        log.info("Start HandleGeneralAddressResponse for digital workflow - iun={} id={}", notification.getIun(), recIndex);
        
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

        log.debug("CheckAddressAndSend - iun={} id={}", iun, recIndex);

        if (digitalAddress != null) {
            log.info("Address with source={} is available, send notification to external channel - iun={} id={}",
                    addressInfo.getDigitalAddressSource(), iun, recIndex);

            //Se l'indirizzo è disponibile, dunque valorizzato viene inviata la notifica a external channel ...
            digitalWorkFlowUtils.addAvailabilitySourceToTimeline(recIndex, notification, addressInfo.getDigitalAddressSource(), true, addressInfo.getSentAttemptMade());
            externalChannelService.sendDigitalNotification(notification, digitalAddress, addressInfo.getDigitalAddressSource(), recIndex, addressInfo.getSentAttemptMade(), false);
        } else {
            //... altrimenti si passa alla prossima workflow action
            log.info("Address with source={} is not available, need to start next workflow action - iun={} id={}",
                    addressInfo.getDigitalAddressSource(), iun, recIndex);
            
            digitalWorkFlowUtils.addAvailabilitySourceToTimeline(recIndex, notification, addressInfo.getDigitalAddressSource(), false, addressInfo.getSentAttemptMade());

            nextWorkFlowAction(notification, recIndex, addressInfo);
        }
    }

    public void handleExternalChannelResponse(ExtChannelDigitalSentResponseInt response) {
        String iun = response.getIun();
        log.debug("Start HandleExternalChannelResponse with eventCode={} - iun={} requestId={}", response.getEventCode(), iun, response.getRequestId());
        
        if( response.getEventCode() != null ){
            
            TimelineElementInternal sendDigitalTimelineElement =
                    digitalWorkFlowUtils.getSendDigitalDetailsTimelineElement(iun, response.getRequestId());

            SendDigitalDetailsInt sendDigitalDetails = (SendDigitalDetailsInt) sendDigitalTimelineElement.getDetails();

            NotificationInt notification = notificationService.getNotificationByIun(iun);
            Integer recIndex = sendDigitalDetails.getRecIndex();

            ResponseStatusInt status = mapDigitalStatusInResponseStatus(response.getEventCode());

            if (status != null)
                handleExternalChannelResponseByStatus(response, iun, sendDigitalTimelineElement, sendDigitalDetails, notification, recIndex, status);
        } else {
            //Se l'evento ricevuto non ha un eventCode valorizzato va ignorato
            log.error("[NOT HANDLED EVENT] Received response haven't value for eventCode, it will be ignored - status={} iun={} requestId={}", response.getStatus(), iun, response.getRequestId());
        }
    }

    private void handleExternalChannelResponseByStatus(ExtChannelDigitalSentResponseInt response, String iun, TimelineElementInternal sendDigitalTimelineElement,
                                                       SendDigitalDetailsInt sendDigitalDetails, NotificationInt notification, Integer recIndex, ResponseStatusInt status) {
        log.debug("Start handleExternalChannelResponseByStatus with status={} eventCode={} - iun={} requestId={}", status, response.getEventCode(), iun, response.getRequestId());

        switch (status) {
            case PROGRESS:
                handleStatusProgress(response, sendDigitalDetails, notification, recIndex, false);
                break;
            case OK:
                handleSuccessfulSending(response, sendDigitalDetails, notification, recIndex, status);
                break;
            case KO:
                handleNotSuccessfulSending(response, sendDigitalTimelineElement, sendDigitalDetails, notification, recIndex, status);
                break;
            case PROGRESS_WITH_RETRY:
                handleStatusProgressWithRetry(response, sendDigitalTimelineElement, sendDigitalDetails, notification, recIndex);
                break;
            default:
                log.error("Status {} is not handled - iun={} id={}", status, iun, recIndex);
                throw new PnInternalException("Status "+ status +" is not handled - iun="+ iun +" id="+ recIndex, ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE);
        }
    }

    private PnAuditLogEvent buildAuditLog( SendDigitalDetailsInt sendDigitalDetails,  ResponseStatusInt status, ExtChannelDigitalSentResponseInt response,  String iun, Integer recIndex){
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();

        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_CHECK, "Digital workflow Ext channel response for source {} retryNumber={} status={} eventCode={} - iun={} id={}",
                        sendDigitalDetails.getDigitalAddressSource(), sendDigitalDetails.getRetryNumber(), status, response.getEventCode(), iun, recIndex)
                .iun(iun)
                .build();
        logEvent.log();
        return logEvent;
    }

    private void handleNotSuccessfulSending(ExtChannelDigitalSentResponseInt response, TimelineElementInternal sendDigitalTimelineElement,
                                            SendDigitalDetailsInt sendDigitalDetails, NotificationInt notification, Integer recIndex,
                                            ResponseStatusInt status) {
        String iun = notification.getIun();

        PnAuditLogEvent logEvent =buildAuditLog(sendDigitalDetails, status, response, iun, recIndex);

        logEvent.generateFailure("Notification failed with eventCode={} eventDetails={}",
                response.getEventCode(), response.getEventDetails()).log();

        log.debug("Response is for 'DELIVERY FAILURE' generatedMessage={} - iun={} id={}", response.getGeneratedMessage(), iun, recIndex);

        digitalWorkFlowUtils.addDigitalFeedbackTimelineElement(notification, status, response.getEventDetails() == null ? Collections.emptyList() : List.of(response.getEventDetails()),
                sendDigitalDetails, response.getGeneratedMessage(), response.getEventTimestamp());

        nextWorkflowStep(sendDigitalTimelineElement, sendDigitalDetails, notification, recIndex);
    }

    private void nextWorkflowStep(TimelineElementInternal sendDigitalTimelineElement, SendDigitalDetailsInt sendDigitalDetails, NotificationInt notification, Integer recIndex) {
        //Non è stato possibile effettuare la notificazione, si passa al prossimo step del workflow

        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .digitalAddressSource(sendDigitalDetails.getDigitalAddressSource())
                .lastAttemptDate(sendDigitalTimelineElement.getTimestamp())
                .build();

        nextWorkFlowAction(notification, recIndex, lastAttemptMade);
    }

    private void handleSuccessfulSending(ExtChannelDigitalSentResponseInt response, SendDigitalDetailsInt sendDigitalDetails, 
                                         NotificationInt notification, Integer recIndex, ResponseStatusInt status) {
        String iun = notification.getIun();

        log.info("Start handleSuccessfulSending with eventCode={} generatedMessage={} - iun={} id={}",  response.getGeneratedMessage(), response.getEventCode(),  iun, recIndex);
        PnAuditLogEvent logEvent = buildAuditLog(sendDigitalDetails, status, response, iun, recIndex);

        //AVVENUTA CONSEGNA

        logEvent.generateSuccess().log();

        digitalWorkFlowUtils.addDigitalFeedbackTimelineElement(notification, status, Collections.emptyList(),
                sendDigitalDetails, response.getGeneratedMessage(), response.getEventTimestamp());

        log.info("Notification sent successfully, starting completion workflow - iun={} id={}",  notification.getIun(), recIndex);

        //La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
        completionWorkflow.completionDigitalWorkflow(notification, recIndex, response.getEventTimestamp(), sendDigitalDetails.getDigitalAddress(), EndWorkflowStatus.SUCCESS);
    }

    private void handleStatusProgress(ExtChannelDigitalSentResponseInt response, SendDigitalDetailsInt sendDigitalDetails,
                                      NotificationInt notification, Integer recIndex, boolean shouldRetry) {
        log.info("Specified status={} is not final - iun={} id={}", response.getStatus(), notification.getIun(), recIndex);

        log.info("Received PROGRESS response with eventCode={} is for PEC acceptance. GeneratedMessage is {} - iun={} id={}",
                response.getEventCode(), response.getGeneratedMessage(), notification.getIun(), recIndex);

        digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(notification, response.getEventCode(),
                sendDigitalDetails.getRecIndex(), sendDigitalDetails.getRetryNumber(), sendDigitalDetails.getDigitalAddress(), sendDigitalDetails.getDigitalAddressSource(),
                shouldRetry, response.getGeneratedMessage(), response.getEventTimestamp());
    }

    private void handleStatusProgressWithRetry(ExtChannelDigitalSentResponseInt response, TimelineElementInternal sendDigitalTimelineElement, SendDigitalDetailsInt sendDigitalDetails,
                                               NotificationInt notification, Integer recIndex)
    {
        // schedulo l'invio per i ritentativi, se vanno fatti in base al numero di fallimenti configurati
        boolean shouldRetry = checkShouldRetry(notification, sendDigitalDetails);

        // aggiungo l'evento di timeline di progress se sono nel caso di retry, o di failure se ho finito i ritentativi
        if (shouldRetry)
        {
            // salvo l'evento come progress
            handleStatusProgress(response, sendDigitalDetails, notification, recIndex, true);

            // è richiesto di ritentare, schedulo un nuovo evento in coda e aggiunto un evento di progress nella timeline
            restartWorkflowAfterRetryTime(notification, recIndex, DigitalAddressInfo.builder()
                .digitalAddress(sendDigitalDetails.getDigitalAddress())
                .digitalAddressSource(sendDigitalDetails.getDigitalAddressSource())
                .lastAttemptDate(response.getEventTimestamp()==null? Instant.now():response.getEventTimestamp())
                .sentAttemptMade(sendDigitalDetails.getRetryNumber().intValue())
                .build());
        }
        else
        {
            // nel caso non siano richiesti tentativi, devo generare un KO definitivo e non un progress
            handleNotSuccessfulSending(response, sendDigitalTimelineElement , sendDigitalDetails, notification, recIndex, ResponseStatusInt.KO);
        }
    }

    private boolean checkShouldRetry(NotificationInt notification, SendDigitalDetailsInt sendDigitalDetails){
        int configCount = pnDeliveryPushConfigs.getExternalChannel().getDigitalRetryCount();
        if (configCount < 0)
            return true;
        if (configCount == 0)
            return false;

        // calcolare in base al numero di tentativi, devo cercare nella timeline quanti retry ci sono stati
        // e se il numero è minore del conteggio richiesto, deve ritentare, altrimenti no
        // la timeline è filtratra per iun, recindex, source, tentativo, quindi identifica i progress di questa istanza di tentativo
        Set<TimelineElementInternal> previousTimelineProgress = digitalWorkFlowUtils.getPreviousTimelineProgress(notification, sendDigitalDetails.getRecIndex(), sendDigitalDetails.getRetryNumber(), sendDigitalDetails.getDigitalAddressSource());
        // il conteggio viene fatto sul flag "retry" a true, visto che comparirà 1 volta per ogni tentativo fallito
        long count = previousTimelineProgress.stream().filter(x -> x.getDetails() instanceof SendDigitalProgressDetailsInt
                                                                && ((SendDigitalProgressDetailsInt)x.getDetails()).isShouldRetry()).count();
        return (count < configCount);
    }

    /**
     * schedule retry for this workflow
     */
    private void restartWorkflowAfterRetryTime(NotificationInt notification, Integer recIndex, DigitalAddressInfo lastAddressInfo) {
        log.debug("restartWorkflowAfterRetryTime - iun={} id={}", notification.getIun(), recIndex);

        String iun = notification.getIun();
        Instant lastAttemptDate = lastAddressInfo.getLastAttemptDate();

        Duration secondNotificationWorkflowWaitingTime = pnDeliveryPushConfigs.getExternalChannel().getDigitalRetryDelay();

        Instant schedulingDate = lastAttemptDate.plus(secondNotificationWorkflowWaitingTime);

        //Vengono aggiunti i minuti necessari
        log.info("Retry workflow scheduling date={} retry workflow - iun={} id={}", schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_RETRY_ACTION);
    }

    private DigitalAddressInfo getDigitalAddressInfo(ScheduleDigitalWorkflowDetailsInt scheduleDigitalWorkflow) {
        return DigitalAddressInfo.builder()
                .digitalAddress(scheduleDigitalWorkflow.getDigitalAddress())
                .digitalAddressSource(scheduleDigitalWorkflow.getDigitalAddressSource())
                .lastAttemptDate(scheduleDigitalWorkflow.getLastAttemptDate())
                .sentAttemptMade(scheduleDigitalWorkflow.getSentAttemptMade())
                .build();
    }

    private ResponseStatusInt mapDigitalStatusInResponseStatus(EventCodeInt eventCode)
    {
        /* Codifica sintetica dello stato dell'esito.
            NB: Lo stato nostro però va calcolato in base alla configurazione, viene quindi ignorato lo status passato da poste
                Questo perchè  ad esempio c008 e c010 son dei progress nel nostro caso
             STATUS            EVENTCODE
            --------              -------------------------------------------
            PROGRESS      C000 = PREACCETTAZIONE (Conferma avvenuta comunicazione con Wrapper Pec) (senza busta)
            PROGRESS      C001 = StatusPec.ACCETTAZIONE (con busta)
            PROGRESS      C005 = StatusPec.PRESA_IN_CARICO (senza busta)
            PROGRESS      C007 = StatusPec.PREAVVISO_ERRORE_CONSEGNA (senza busta)
            ERROR         C002 = StatusPec.NON_ACCETTAZIONE (con busta)
            ERROR         C004 = StatusPec.ERReveORE_CONSEGNA (con busta)
            ERROR         C006 = StatusPec.RILEVAZIONE_VIRUS (con busta)
            ERROR         C008 = ERRORE_COMUNICAZIONE_SERVER_PEC - con retry  (senza busta)
            ERROR         C009   = ERRORE_DOMINIO_PEC_NON_VALIDO - senza retry:  indica un dominio pec non valido; (senza busta)
            ERROR         C0010 = ERROR_INVIO_PEC - con retry da parte di PN: indica un errore generico di invio pec (senza busta)
            OK            C003 = StatusPec.AVVENUTA_CONSEGNA (con busta)
        */
        if (eventCode == null)
            throw new PnInternalException("Invalid received digital status:" + eventCode, ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE);

        String eventCodeValue = eventCode.getValue();

        PnDeliveryPushConfigs.ExternalChannel externalChannelConfig = this.pnDeliveryPushConfigs.getExternalChannel();
        if (externalChannelConfig.getDigitalCodesFatallog().contains(eventCodeValue)){
            log.error("FATAL!!!!: received eventcode {} from external-channel, should check why!!", eventCodeValue);
        }

        if (externalChannelConfig.getDigitalCodesProgress().contains(eventCodeValue)){
            return ResponseStatusInt.PROGRESS;
        }
        if (externalChannelConfig.getDigitalCodesSuccess().contains(eventCodeValue)){
            return ResponseStatusInt.OK;
        }
        if (externalChannelConfig.getDigitalCodesFail().contains(eventCodeValue)){
            return  ResponseStatusInt.KO;
        }
        if (externalChannelConfig.getDigitalCodesRetryable().contains(eventCodeValue)){
            return  ResponseStatusInt.PROGRESS_WITH_RETRY;
        }

        log.info("received eventcode {} from external-channel, will be simply skipped because not PROGRESS/OK/KO", eventCodeValue);
        return null;
    }
    
}
