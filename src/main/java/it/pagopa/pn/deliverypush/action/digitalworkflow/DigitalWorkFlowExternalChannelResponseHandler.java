package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.DigitalMessageReferenceInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalSendTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalProgressDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SENDDIGITALTIMELINEEVENTNOTFOUND;

@Component
@AllArgsConstructor
@Slf4j
public class DigitalWorkFlowExternalChannelResponseHandler { 
 
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final CompletionWorkFlowHandler completionWorkflow; 
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AuditLogService auditLogService;
    private final SendAndUnscheduleNotification sendAndUnscheduleNotification;
    

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
            
            //Nota qui dovrebbe essere sempre un caso di sendDigital o digitalProgress
            if (timelineElement.getDetails() instanceof DigitalSendTimelineElementDetails sendDigitalDetailsInt)
            {
                digitalResultInfos.setRecIndex(sendDigitalDetailsInt.getRecIndex());
                digitalResultInfos.setRetryNumber(sendDigitalDetailsInt.getRetryNumber());
                digitalResultInfos.setDigitalAddressInt(sendDigitalDetailsInt.getDigitalAddress());
                digitalResultInfos.setDigitalAddressSourceInt(sendDigitalDetailsInt.getDigitalAddressSource());
                digitalResultInfos.setIsFirstSendRetry(sendDigitalDetailsInt.getIsFirstSendRetry());
                digitalResultInfos.setRelatedFeedbackTimelineElement(sendDigitalDetailsInt.getRelatedFeedbackTimelineId());
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
            case PROGRESS -> handleStatusProgress(digitalResultInfos, false);
            case OK -> handleSuccessfulSending(digitalResultInfos);
            case KO -> handleNotSuccessfulSending(digitalResultInfos);
            case PROGRESS_WITH_RETRY -> handleStatusProgressWithRetry(digitalResultInfos);
            default -> {
                log.error("Status {} is not handled - iun={} id={}", digitalResultInfos.getStatus(), digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());
                throw new PnInternalException("Status " + digitalResultInfos.getStatus() + " is not handled - iun=" + digitalResultInfos.getNotification().getIun() + " id=" + digitalResultInfos.getRecIndex(), ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE);
            }
        }
    }

    private PnAuditLogEvent buildAuditLog(  DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos){
        DigitalMessageReferenceInt digitalMessageReference = digitalResultInfos.getResponse().getGeneratedMessage();
        String attachments = (digitalMessageReference!=null && digitalMessageReference.getLocation()!=null)?digitalMessageReference.getLocation():"";

        return auditLogService.buildAuditLogEvent(digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex(),
                PnAuditLogEventType.AUD_DD_RECEIVE,"Digital workflow Ext channel response for source {} retryNumber={} status={} eventCode={} attachments={}",
                digitalResultInfos.getDigitalAddressSourceInt(), digitalResultInfos.getRetryNumber(), digitalResultInfos.getStatus(),
                digitalResultInfos.getResponse().getEventCode() , attachments);
    }

    void handleNotSuccessfulSending(DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos) {
        String iun = digitalResultInfos.getNotification().getIun();

        PnAuditLogEvent logEvent =buildAuditLog( digitalResultInfos );

        try {
            log.debug("Response is for 'DELIVERY FAILURE' generatedMessage={} - iun={} id={}", digitalResultInfos.getResponse().getGeneratedMessage(), iun, digitalResultInfos.getRecIndex());

            // unschedulo eventuale timer programmato di invio
            sendAndUnscheduleNotification.unscheduleTimeoutAction(iun, digitalResultInfos.getRecIndex(), digitalResultInfos.getTimelineElementInternal()==null?null:digitalResultInfos.getTimelineElementInternal().getElementId());

            SendInformation digitalAddressFeedback = SendInformation.builder()
                    .retryNumber(digitalResultInfos.getRetryNumber())
                    .eventTimestamp(digitalResultInfos.getResponse().getEventTimestamp())
                    .digitalAddressSource(digitalResultInfos.getDigitalAddressSourceInt())
                    .digitalAddress(digitalResultInfos.getDigitalAddressInt())
                    .build();
        
           String timelineId = digitalWorkFlowUtils.addDigitalFeedbackTimelineElement(
                   digitalResultInfos.getTimelineElementInternal()==null?"":digitalResultInfos.getTimelineElementInternal().getElementId(), 
                   digitalResultInfos.getNotification(), 
                   digitalResultInfos.getStatus(), 
                   digitalResultInfos.getRecIndex(), 
                   digitalResultInfos.getResponse(), 
                   digitalAddressFeedback, 
                   digitalResultInfos.getIsFirstSendRetry()
            );

            if(digitalResultInfos.getIsFirstSendRetry() != null && digitalResultInfos.getIsFirstSendRetry()){
                //Si tratta della response al primo invio degli eventuali due invii (relativi al secondo ciclo di notifica),
                // c'è da fare un ulteriore tentativo all'indirizzo recuperato da Banca dati se disponibile
                DigitalAddressInfoSentAttempt addressInfo = DigitalAddressInfoSentAttempt.builder()
                        .digitalAddressSource(digitalResultInfos.getDigitalAddressSourceInt())
                        .lastAttemptDate(digitalResultInfos.getTimelineElementInternal().getTimestamp())
                        .sentAttemptMade(digitalResultInfos.getRetryNumber())
                        .relatedFeedbackTimelineId(timelineId)
                        .build();
                NotificationInt notification = notificationService.getNotificationByIun(iun);

                digitalWorkFlowHandler.checkAndSendNotification(notification, digitalResultInfos.getRecIndex(), addressInfo);
            } else {
                //devo verificare se si tratta del secondo tentativo relativo al secondo ciclo di notifica per una determinata source
                if(digitalResultInfos.getRelatedFeedbackTimelineElement() != null){
                    //verifico se il primo tentativo è andato a buon fine ed eventualmente completo il workflow con successo
                    boolean completedWorkflowSuccess = digitalWorkFlowHandler.checkFirstAttemptAndCompleteWorkflow(
                            digitalResultInfos.getNotification(), digitalResultInfos.getRecIndex(), digitalResultInfos.getRelatedFeedbackTimelineElement(), iun);
                    if(! completedWorkflowSuccess){
                        //Se il primo tentativo NON è andato a buon fine si passa al prossimo step del workflow
                        digitalWorkFlowHandler.nextWorkflowStep( digitalResultInfos );
                    }
                } else {
                    //sono nella risposta negativa di un tentativo classico
                    digitalWorkFlowHandler.nextWorkflowStep( digitalResultInfos );
                }
            }
            
            logEvent.generateWarning("Digital notification failed with eventCode={} eventDetails={} timelineId={}",
                    digitalResultInfos.getResponse().getEventCode(), digitalResultInfos.getResponse().getEventDetails(), timelineId).log();

        } catch (Exception e) {
            logEvent.generateFailure("Error handleNotSuccessfulSending exc={}", e).log();
           throw e;
        }
    }
    
    private void handleSuccessfulSending( DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos ) {
        String iun = digitalResultInfos.getNotification().getIun();

        log.info("Start handleSuccessfulSending with eventCode={} generatedMessage={} - iun={} id={}",  digitalResultInfos.getResponse().getGeneratedMessage(), digitalResultInfos.getResponse().getEventCode(),  iun, digitalResultInfos.getRecIndex());
        PnAuditLogEvent logEvent = buildAuditLog( digitalResultInfos );

        try {
            //AVVENUTA CONSEGNA

            // unschedulo eventuale timer programmato di invio
            sendAndUnscheduleNotification.unscheduleTimeoutAction(iun, digitalResultInfos.getRecIndex(), digitalResultInfos.getTimelineElementInternal()==null?null:digitalResultInfos.getTimelineElementInternal().getElementId());

            SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(digitalResultInfos.getRetryNumber())
                .eventTimestamp(digitalResultInfos.getResponse().getEventTimestamp())
                .digitalAddressSource(digitalResultInfos.getDigitalAddressSourceInt())
                .digitalAddress(digitalResultInfos.getDigitalAddressInt())
                .build();
        
            String sendDigitalFeedbackTimelineId = digitalWorkFlowUtils.addDigitalFeedbackTimelineElement(
                digitalResultInfos.getTimelineElementInternal()==null?"":digitalResultInfos.getTimelineElementInternal().getElementId(),
                digitalResultInfos.getNotification(),
                digitalResultInfos.getStatus(),
                digitalResultInfos.getRecIndex(),
                    digitalResultInfos.getResponse(),
                digitalAddressFeedback,
                    digitalResultInfos.getIsFirstSendRetry()
            );

            log.info("Notification sent successfully, starting completion workflow - iun={} id={}",  digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());
            
            if(digitalResultInfos.getIsFirstSendRetry() != null && digitalResultInfos.getIsFirstSendRetry()){
                log.info("Is response for firstSendRetry - iun={} id={}",  digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());
                //Si tratta della response al primo invio degli eventuali due invii (relativi al secondo ciclo di notifica),
                // c'è da fare un ulteriore tentativo all'indirizzo recuperato da Banca dati se disponibile
                DigitalAddressInfoSentAttempt addressInfo = DigitalAddressInfoSentAttempt.builder()
                        .digitalAddressSource(digitalResultInfos.getDigitalAddressSourceInt())
                        .lastAttemptDate(digitalResultInfos.getTimelineElementInternal().getTimestamp())
                        .sentAttemptMade(digitalResultInfos.getRetryNumber())
                        .relatedFeedbackTimelineId(sendDigitalFeedbackTimelineId)
                        .build();
                NotificationInt notification = notificationService.getNotificationByIun(iun);
                
                digitalWorkFlowHandler.checkAndSendNotification(notification, digitalResultInfos.getRecIndex(), addressInfo);
                logEvent.generateSuccess("Pec sent successfully, but need to sent another PEC if address is available").log();
            } else {
                log.info("Is not response for firstSendRetry - iun={} id={}",  digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());

                //Se si tratta di un feedback classico, piuttosto che il feedaback al secondo tentativo del secondo ciclo d'invii per una source, porto la notifica in accettata
                //perchè sono sicuro che almeno questo feedback è positivo, non mi interessa dunque di controllare il primo feedback
                
                //La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                String timelineId = completionWorkflow.completionDigitalWorkflow(
                        digitalResultInfos.getNotification(),
                        digitalResultInfos.getRecIndex(),
                        digitalResultInfos.getResponse().getEventTimestamp(),
                        digitalResultInfos.getDigitalAddressInt(),
                        EndWorkflowStatus.SUCCESS
                );

                logEvent.generateSuccess("Pec sent successfully, completion workflow timelineId={}", timelineId).log();
            }
            
        } catch (Exception e) {
            logEvent.generateFailure("Error handleSuccessfulSending exc={}", e).log();
            throw e;
        }
    }

    void handleStatusProgress(DigitalWorkFlowHandler.DigitalResultInfos digitalResultInfos, boolean shouldRetry) {
        log.info("Specified status={} is not final - iun={} id={}", digitalResultInfos.getResponse().getStatus(), digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());

        log.info("Received PROGRESS response with eventCode={}  GeneratedMessage is {} - iun={} id={}",
                digitalResultInfos.getResponse().getEventCode(), digitalResultInfos.getResponse().getGeneratedMessage(), digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex());
        
        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(digitalResultInfos.getRetryNumber())
                .eventTimestamp(digitalResultInfos.getResponse().getEventTimestamp())
                .digitalAddressSource(digitalResultInfos.getDigitalAddressSourceInt())
                .digitalAddress(digitalResultInfos.getDigitalAddressInt())
                .isFirstSendRetry(digitalResultInfos.getIsFirstSendRetry())
                .relatedFeedbackTimelineId(digitalResultInfos.getRelatedFeedbackTimelineElement())
                .build();
        
        digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(
                digitalResultInfos.getNotification(),
                digitalResultInfos.getResponse().getEventCode(),
                digitalResultInfos.getRecIndex(),
                shouldRetry, 
                digitalResultInfos.getResponse().getGeneratedMessage(),
                digitalAddressFeedback
        );
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
            sendAndUnscheduleNotification.unscheduleTimeoutAction(digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex(), digitalResultInfos.getTimelineElementInternal()==null?null:digitalResultInfos.getTimelineElementInternal().getElementId());

            // è richiesto di ritentare, schedulo un nuovo evento in coda e aggiunto un evento di progress nella timeline
            final DigitalAddressInfoSentAttempt addressInfo = DigitalAddressInfoSentAttempt.builder()
                    .digitalAddress(digitalResultInfos.getDigitalAddressInt())
                    .digitalAddressSource(digitalResultInfos.getDigitalAddressSourceInt())
                    .lastAttemptDate(digitalResultInfos.getResponse().getEventTimestamp() == null ? Instant.now() : digitalResultInfos.getResponse().getEventTimestamp())
                    .sentAttemptMade(digitalResultInfos.getRetryNumber())
                    .build();
            
            restartWorkflowAfterRetryTime(
                    digitalResultInfos.getNotification(),
                    digitalResultInfos.getRecIndex(),
                    addressInfo, 
                    digitalResultInfos.getTimelineElementInternal()
            );
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
        Set<TimelineElementInternal> previousTimelineProgress = digitalWorkFlowUtils.getPreviousTimelineProgress(digitalResultInfos.getNotification(), digitalResultInfos.getRecIndex(),
                digitalResultInfos.getRetryNumber(), digitalResultInfos.getIsFirstSendRetry(), digitalResultInfos.getDigitalAddressSourceInt());
        // il conteggio viene fatto sul flag "retry" a true, visto che comparirà 1 volta per ogni tentativo fallito
        long count = previousTimelineProgress.stream().filter(x -> x.getDetails() instanceof SendDigitalProgressDetailsInt sendDigitalProgressDetailsInt
                                                                && sendDigitalProgressDetailsInt.isShouldRetry()).count();
        return (count < configCount);
    }

    /**
     * schedule retry for this workflow
     */
    private void restartWorkflowAfterRetryTime(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt lastAddressInfo, TimelineElementInternal referenceTimelineId) {
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
