package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalSendTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalProgressDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SENDDIGITALTIMELINEEVENTNOTFOUND;

@Component
@Slf4j
public class DigitalWorkFlowExternalChannelResponseHandler { 
 
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final CompletionWorkFlowHandler completionWorkflow; 
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;

    public DigitalWorkFlowExternalChannelResponseHandler(NotificationService notificationService,
                                                         SchedulerService schedulerService,
                                                         DigitalWorkFlowUtils digitalWorkFlowUtils,
                                                         CompletionWorkFlowHandler completionWorkflow,
                                                         PnDeliveryPushConfigs pnDeliveryPushConfigs, DigitalWorkFlowHandler digitalWorkFlowHandler) {
        this.notificationService = notificationService;
        this.schedulerService = schedulerService;
        this.digitalWorkFlowUtils = digitalWorkFlowUtils;
        this.completionWorkflow = completionWorkflow;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
    }
 

    /**
     * Callback nel caso di evento ricevuto da ext-channel
     * @param response evento di ext-channel
     */
    public void handleExternalChannelResponse(ExtChannelDigitalSentResponseInt response) {
        String iun = response.getIun();
        log.debug("Start HandleExternalChannelResponse with eventCode={} - iun={} requestId={}", response.getEventCode(), iun, response.getRequestId());

        if( response.getEventCode() != null ){

            TimelineElementInternal timelineElement = getSendDigitalDetailsTimelineElement(iun, response.getRequestId());

            // creo una struttura dati di supporto per passare i vari valori nei metodi INTERNI della classe
            DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos = new DigitalWorkFlowHandler.DigitalResultInfos();
            digitalResultInfos.setResponse(response);
            digitalResultInfos.setTimelineElementInternal(timelineElement);
            if (timelineElement.getDetails() instanceof DigitalSendTimelineElementDetails)
            {
                DigitalSendTimelineElementDetails sendDigitalProgressDetailsInt = (DigitalSendTimelineElementDetails) timelineElement.getDetails();
                digitalResultInfos.setRecIndex(sendDigitalProgressDetailsInt.getRecIndex());
                digitalResultInfos.setRetryNumber(sendDigitalProgressDetailsInt.getRetryNumber());
                digitalResultInfos.setDigitalAddressInt(sendDigitalProgressDetailsInt.getDigitalAddress());
                digitalResultInfos.setDigitalAddressSourceInt(sendDigitalProgressDetailsInt.getDigitalAddressSource());
            }

            digitalResultInfos.setNotification(notificationService.getNotificationByIun(iun));

            digitalResultInfos.setStatus(mapDigitalStatusInResponseStatus(response.getEventCode()));

            if (digitalResultInfos.getStatus() != null)
                handleExternalChannelResponseByStatus(digitalResultInfos);
        } else {
            //Se l'evento ricevuto non ha un eventCode valorizzato va ignorato
            log.error("[NOT HANDLED EVENT] Received response haven't value for eventCode, it will be ignored - status={} iun={} requestId={}", response.getStatus(), iun, response.getRequestId());
        }
    }


    private void handleExternalChannelResponseByStatus( DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos ) {
        log.debug("Start handleExternalChannelResponseByStatus with status={} eventCode={} - iun={} requestId={}", digitalResultInfos.getStatus(), digitalResultInfos.getResponse().getEventCode(), digitalResultInfos.getNotification().getIun(), digitalResultInfos.getResponse().getRequestId());

        switch (digitalResultInfos.getStatus()) {
            case PROGRESS:
                handleStatusProgress(digitalResultInfos, false);
                break;
            case OK:
                handleSuccessfulSending(digitalResultInfos);
                break;
            case KO:
                handleNotSuccessfulSending(digitalResultInfos);
                break;
            case PROGRESS_WITH_RETRY:
                handleStatusProgressWithRetry(digitalResultInfos);
                break;
            default:
                log.error("Status {} is not handled - iun={} id={}", digitalResultInfos.getStatus(), digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());
                throw new PnInternalException("Status "+ digitalResultInfos.getStatus() +" is not handled - iun="+ digitalResultInfos.getNotification().getIun() +" id="+ digitalResultInfos.getRecIndex(), ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE);
        }
    }

    private PnAuditLogEvent buildAuditLog(  DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos ){
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();

        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_CHECK, "Digital workflow Ext channel response for source {} retryNumber={} status={} eventCode={} - iun={} id={}",
                        digitalResultInfos.getDigitalAddressSourceInt(), digitalResultInfos.getRetryNumber(), digitalResultInfos.getStatus(), digitalResultInfos.getResponse().getEventCode(), digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex())
                .iun(digitalResultInfos.getNotification().getIun())
                .build();
        logEvent.log();
        return logEvent;
    }

    void handleNotSuccessfulSending(DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos) {
        String iun = digitalResultInfos.getNotification().getIun();

        PnAuditLogEvent logEvent =buildAuditLog( digitalResultInfos );

        logEvent.generateFailure("Notification failed with eventCode={} eventDetails={}",
                digitalResultInfos.getResponse().getEventCode(), digitalResultInfos.getResponse().getEventDetails()).log();

        log.debug("Response is for 'DELIVERY FAILURE' generatedMessage={} - iun={} id={}", digitalResultInfos.getResponse().getGeneratedMessage(), iun, digitalResultInfos.getRecIndex());

        // unschedulo eventuale timer programmato di invio
        digitalWorkFlowHandler.unscheduleTimeoutAction(iun, digitalResultInfos.getRecIndex(), digitalResultInfos.getTimelineElementInternal()==null?null:digitalResultInfos.getTimelineElementInternal().getElementId());

        digitalWorkFlowUtils.addDigitalFeedbackTimelineElement(digitalResultInfos.getNotification(), digitalResultInfos.getStatus(), digitalResultInfos.getResponse().getEventDetails() == null ? new ArrayList<>() : List.of(digitalResultInfos.getResponse().getEventDetails()),
                digitalResultInfos.getRecIndex(), digitalResultInfos.getRetryNumber(), digitalResultInfos.getDigitalAddressInt(), digitalResultInfos.getDigitalAddressSourceInt(), digitalResultInfos.getResponse().getGeneratedMessage(), digitalResultInfos.getResponse().getEventTimestamp());

        digitalWorkFlowHandler.nextWorkflowStep( digitalResultInfos );
    }


    private void handleSuccessfulSending( DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos ) {
        String iun = digitalResultInfos.getNotification().getIun();

        log.info("Start handleSuccessfulSending with eventCode={} generatedMessage={} - iun={} id={}",  digitalResultInfos.getResponse().getGeneratedMessage(), digitalResultInfos.getResponse().getEventCode(),  iun, digitalResultInfos.getRecIndex());
        PnAuditLogEvent logEvent = buildAuditLog( digitalResultInfos );

        //AVVENUTA CONSEGNA

        logEvent.generateSuccess().log();

        // unschedulo eventuale timer programmato di invio
        digitalWorkFlowHandler.unscheduleTimeoutAction(iun, digitalResultInfos.getRecIndex(), digitalResultInfos.getTimelineElementInternal()==null?null:digitalResultInfos.getTimelineElementInternal().getElementId());

        digitalWorkFlowUtils.addDigitalFeedbackTimelineElement(digitalResultInfos.getNotification(), digitalResultInfos.getStatus(), Collections.emptyList(),
                digitalResultInfos.getRecIndex(), digitalResultInfos.getRetryNumber(), digitalResultInfos.getDigitalAddressInt(), digitalResultInfos.getDigitalAddressSourceInt(), digitalResultInfos.getResponse().getGeneratedMessage(), digitalResultInfos.getResponse().getEventTimestamp());

        log.info("Notification sent successfully, starting completion workflow - iun={} id={}",  digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());

        //La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
        completionWorkflow.completionDigitalWorkflow(
                digitalResultInfos.getNotification(),
                digitalResultInfos.getRecIndex(),
                digitalResultInfos.getResponse().getEventTimestamp(),
                digitalResultInfos.getDigitalAddressInt(),
                EndWorkflowStatus.SUCCESS
        );
    }

    void handleStatusProgress(DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos, boolean shouldRetry) {
        log.info("Specified status={} is not final - iun={} id={}", digitalResultInfos.getResponse().getStatus(), digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());

        log.info("Received PROGRESS response with eventCode={}  GeneratedMessage is {} - iun={} id={}",
                digitalResultInfos.getResponse().getEventCode(), digitalResultInfos.getResponse().getGeneratedMessage(), digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());

        digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(digitalResultInfos.getNotification(), digitalResultInfos.getResponse().getEventCode(),
                digitalResultInfos.getRecIndex(), digitalResultInfos.getRetryNumber(), digitalResultInfos.getDigitalAddressInt(), digitalResultInfos.getDigitalAddressSourceInt(),
                shouldRetry, digitalResultInfos.getResponse().getGeneratedMessage(), digitalResultInfos.getResponse().getEventTimestamp());
    }

    private void handleStatusProgressWithRetry( DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos )
    {
        // schedulo l'invio per i ritentativi, se vanno fatti in base al numero di fallimenti configurati
        boolean shouldRetry = checkShouldRetry(digitalResultInfos);

        // aggiungo l'evento di timeline di progress se sono nel caso di retry, o di failure se ho finito i ritentativi
        if (shouldRetry)
        {
            // salvo l'evento come progress
            handleStatusProgress(digitalResultInfos, true);

            // unschedulo eventuale timer programmato di invio
            digitalWorkFlowHandler.unscheduleTimeoutAction(digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex(), digitalResultInfos.getTimelineElementInternal()==null?null:digitalResultInfos.getTimelineElementInternal().getElementId());

            // è richiesto di ritentare, schedulo un nuovo evento in coda e aggiunto un evento di progress nella timeline
            restartWorkflowAfterRetryTime(digitalResultInfos.getNotification(), digitalResultInfos.getRecIndex(), DigitalAddressInfo.builder()
                .digitalAddress(digitalResultInfos.getDigitalAddressInt())
                .digitalAddressSource(digitalResultInfos.getDigitalAddressSourceInt())
                .lastAttemptDate(digitalResultInfos.getResponse().getEventTimestamp()==null? Instant.now():digitalResultInfos.getResponse().getEventTimestamp())
                .sentAttemptMade(digitalResultInfos.getRetryNumber())
                .build(), digitalResultInfos.getTimelineElementInternal());
        }
        else
        {
            // nel caso non siano richiesti tentativi, devo generare un KO definitivo e non un progress
            digitalResultInfos.setStatus(ResponseStatusInt.KO);
            handleNotSuccessfulSending(digitalResultInfos);
        }
    }

    private boolean checkShouldRetry( DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos ){
        int configCount = pnDeliveryPushConfigs.getExternalChannel().getDigitalRetryCount();
        if (configCount < 0)
            return true;
        if (configCount == 0)
            return false;

        // calcolare in base al numero di tentativi, devo cercare nella timeline quanti retry ci sono stati
        // e se il numero è minore del conteggio richiesto, deve ritentare, altrimenti no
        // la timeline è filtratra per iun, recindex, source, tentativo, quindi identifica i progress di questa istanza di tentativo
        Set<TimelineElementInternal> previousTimelineProgress = digitalWorkFlowUtils.getPreviousTimelineProgress(digitalResultInfos.getNotification(), digitalResultInfos.getRecIndex(), digitalResultInfos.getRetryNumber(), digitalResultInfos.getDigitalAddressSourceInt());
        // il conteggio viene fatto sul flag "retry" a true, visto che comparirà 1 volta per ogni tentativo fallito
        long count = previousTimelineProgress.stream().filter(x -> x.getDetails() instanceof SendDigitalProgressDetailsInt
                                                                && ((SendDigitalProgressDetailsInt)x.getDetails()).isShouldRetry()).count();
        return (count < configCount);
    }

    /**
     * schedule retry for this workflow
     */
    private void restartWorkflowAfterRetryTime(NotificationInt notification, Integer recIndex, DigitalAddressInfo lastAddressInfo, TimelineElementInternal referenceTimelineId) {
        log.debug("restartWorkflowAfterRetryTime - iun={} id={}", notification.getIun(), recIndex);

        String iun = notification.getIun();
        Instant lastAttemptDate = lastAddressInfo.getLastAttemptDate();

        Duration secondNotificationWorkflowWaitingTime = pnDeliveryPushConfigs.getExternalChannel().getDigitalRetryDelay();

        Instant schedulingDate = lastAttemptDate.plus(secondNotificationWorkflowWaitingTime);

        //Vengono aggiunti i minuti necessari
        //NB: il referenceTimelineId è l'id dell'elemento di timeline che ha dato luogo all'invio della notifica (potrebbe essere un send_digital_domicile o un send_digital_progress con un certo prog_id)
        log.info("Retry workflow scheduling date={} retry workflow - iun={} id={}", schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_RETRY_ACTION, referenceTimelineId.getElementId());
    }


    private TimelineElementInternal getSendDigitalDetailsTimelineElement(String iun, String eventId) {

        Optional<TimelineElementInternal> sendDigitalTimelineElement = digitalWorkFlowUtils.getTimelineElement(iun, eventId);

        if (sendDigitalTimelineElement.isPresent()) {
            return sendDigitalTimelineElement.get();
        } else {
            String error = String.format("SendDigital timeline element not exist -iun=%s requestId=%s", iun, eventId);
            log.error(error);
            throw new PnInternalException(error, ERROR_CODE_DELIVERYPUSH_SENDDIGITALTIMELINEEVENTNOTFOUND);
        }
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

            BONUS:
            PROGRESS      DP10 = Generato da timeout di invio a ext-channel, in caso di mancata risposta.
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
