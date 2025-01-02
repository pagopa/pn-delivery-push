package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelProgressEventCat;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalSendTimelineElementDetails;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class DigitalWorkFlowRetryHandler {

    private final NotificationService notificationService;

    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final SendAndUnscheduleNotification sendAndUnscheduleNotification;
    private final DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowExternalChannelResponseHandler;
    private final FeatureEnabledUtils featureEnabledUtils;

    /**
     * Callback nel caso di ritentativo a breve termine di invio PEC
     * @param iun IUN della notifica
     * @param recIndex id recipient
     * @param timelineId id timeline che ha generato la schedulazione
     */
    public void startScheduledRetryWorkflow(String iun, Integer recIndex, String timelineId) {
        log.debug("startScheduledRetryWorkflow - iun={} recIndex={} timelineId={}", iun, recIndex, timelineId);
        
        //Viene recuperato l'evento di timeline di SEND o eventualmente di Progress se si tratta di un retry
        Optional<TimelineElementInternal> timelineElement = digitalWorkFlowUtils.getTimelineElement(iun, timelineId);
        
        if (timelineElement.isPresent() && timelineElement.get().getDetails() instanceof DigitalSendTimelineElementDetails originalSendDigitalProgressDetailsInt) {
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            if (checkIfEventIsStillValid(iun, timelineElement.get())) {
                sendAndUnscheduleNotification.sendDigitalNotificationAndScheduleTimeoutAction(
                        notification,
                        originalSendDigitalProgressDetailsInt.getDigitalAddress(),
                        DigitalAddressInfoSentAttempt.builder()
                                .digitalAddress(originalSendDigitalProgressDetailsInt.getDigitalAddress())
                                .digitalAddressSource(originalSendDigitalProgressDetailsInt.getDigitalAddressSource())
                                .lastAttemptDate(timelineElement.get().getTimestamp())
                                .sentAttemptMade(originalSendDigitalProgressDetailsInt.getRetryNumber())
                                .relatedFeedbackTimelineId(originalSendDigitalProgressDetailsInt.getRelatedFeedbackTimelineId())
                                .build(), 
                        recIndex, 
                        true,
                        timelineId,
                        originalSendDigitalProgressDetailsInt.getIsFirstSendRetry());
            }
            else
            {
                log.info("startScheduledRetryWorkflow ABORTED because last status is not send or progress iun={} recIndex={}", iun, recIndex);
            }
        }
        else
        {
            log.error("startScheduledRetryWorkflow ABORTED because UNEXPECTED timeline generation event iun={} recIndex={} timelineId={}", iun, recIndex, timelineElement.isPresent()?timelineElement.get().getElementId():"notfound!");
        }
    }


    /**
     * Callback nel caso di timeout di mancata ricezione risultati da ext-channel.
     * Recupera la timeline che l'ha generato, e controlla che l'ultima timeline per il recipient sia dello stesso "tentativo" di quella che l'ha generata.
     * Se nel frattempo sono arrivate risposte o perfezionamenti, il metodo logga senza fare altro.
     * Altrimenti, da il via ad un nuovo tentativo di invio secondo schedulazione
     *
     * @param iun IUN della notifica
     * @param recIndex id recipient
     * @param timelineId id timeline che ha generato la schedulazione
     */
    public void elapsedExtChannelTimeout(String iun, int recIndex, String timelineId) {
        log.info("elapsedExtChannelTimeout - iun={} recIndex={} timelineId={}", iun, recIndex, timelineId);

        // recupero il timelineId originale, del quale mi interessano retrynumber e addresssource
        Optional<TimelineElementInternal> timelineElement =
                digitalWorkFlowUtils.getTimelineElement(iun, timelineId);

        if (timelineElement.isEmpty())
        {
            // caso decisamente strano...loggo ma non tiro errore perchè tanto, se non c'è l'evento non è che ritentando risolve
            log.error("elapsedExtChannelTimeout Original timelineevent not found, skipping actions iun={} recIdx={}", iun, recIndex);
            return;
        }

        Integer originalRetryNumber = -1;
        DigitalAddressSourceInt originalAddressSource = null;
        LegalDigitalAddressInt originalAddressInfo = null;

        if (timelineElement.get().getDetails() instanceof DigitalSendTimelineElementDetails sendDigitalProgressDetailsInt)
        {
            // dovrebbe sempre essere instanceof di questo tipo
            originalRetryNumber = sendDigitalProgressDetailsInt.getRetryNumber();
            originalAddressSource = sendDigitalProgressDetailsInt.getDigitalAddressSource();
            originalAddressInfo = sendDigitalProgressDetailsInt.getDigitalAddress();
        }
        else
        {
            // caso decisamente strano...loggo ma non tiro errore perchè tanto, se non c'è l'evento non è che ritentando risolve
            log.error("elapsedExtChannelTimeout Original timelineevent not found, skipping actions iun={} recIdx={}", iun, recIndex);
        }

        // devo controllare che il timeout scattato sia ancora rilevante.
        if (checkIfEventIsStillValid(iun, timelineElement.get()))
        {
            // Effettuo un nuovo tentativo d'invio verso externalChannel
            resendNewEventoToExtChannel(iun, recIndex, timelineId);
        }
        else {
            // salvo cmq in timeline il fatto che ho deciso di non rischedulare i tentativi
            handleDontNeddToRetryCase(iun, recIndex, originalRetryNumber, originalAddressSource, originalAddressInfo);
        }
    }

    private void resendNewEventoToExtChannel(String iun, int recIndex, String timelineId) {
        log.info("Timeout expired and need to send new attempt - iun={} recIdx={}", iun, recIndex);
        // se lo è schedulo gestisco secondo configurazione
        // EMULO la response proveniente da ext-channel, così poi la logica sarà la stessa
        digitalWorkFlowExternalChannelResponseHandler.handleExternalChannelResponse(ExtChannelDigitalSentResponseInt.builder()
                .eventCode(EventCodeInt.DP10)
                .requestId(timelineId)
                .iun(iun)
                .eventTimestamp(Instant.now())
                .status(ExtChannelProgressEventCat.PROGRESS)
                .eventDetails("expired timeout")
                .build());
    }

    private void handleDontNeddToRetryCase(String iun, int recIndex, Integer originalRetryNumber, DigitalAddressSourceInt originalAddressSource, LegalDigitalAddressInt originalAddressInfo) {
        // salvo cmq in timeline il fatto che ho deciso di non rischedulare i tentativi

        NotificationInt notification = notificationService.getNotificationByIun(iun);

        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(originalRetryNumber)
                .eventTimestamp(Instant.now())
                .digitalAddressSource(originalAddressSource)
                .digitalAddress(originalAddressInfo)
                .isFirstSendRetry(null)
                .relatedFeedbackTimelineId(null)
                .build();

        digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(notification,
                EventCodeInt.DP10,
                recIndex,
                false,
                null,
                digitalAddressFeedback);

        log.info("elapsedExtChannelTimeout but don't need to retry, skipping more actions iun={} recIdx={}", iun, recIndex);
    }

    private boolean checkIfEventIsStillValid(String iun, TimelineElementInternal originalTimelineElement){
        //L'evento viene definito non valido (dunque l'invio si è concluso) se esiste un SEND_DIGITAL_FEEDBACK relativo a tale evento, al contrario risulta ancora valido
        if (originalTimelineElement.getDetails() instanceof DigitalSendTimelineElementDetails originalDigitalSendTimelineDetailsInt) {
            Optional<TimelineElementInternal> sendDigitalFeedbackOpt = digitalWorkFlowUtils.getSendDigitalFeedbackFromSourceTimeline(iun, originalDigitalSendTimelineDetailsInt);
            return sendDigitalFeedbackOpt.isEmpty();
        }
        return false;
    }


}
