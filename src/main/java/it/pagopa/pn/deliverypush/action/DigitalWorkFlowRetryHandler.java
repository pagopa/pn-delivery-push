package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressFeedback;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelProgressEventCat;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalSendTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class DigitalWorkFlowRetryHandler {

    private final NotificationService notificationService;

    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowExternalChannelResponseHandler;

    public DigitalWorkFlowRetryHandler(DigitalWorkFlowHandler digitalWorkFlowHandler,
                                       NotificationService notificationService,
                                       DigitalWorkFlowUtils digitalWorkFlowUtils, DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowExternalChannelResponseHandler) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.notificationService = notificationService;
        this.digitalWorkFlowUtils = digitalWorkFlowUtils;
        this.digitalWorkFlowExternalChannelResponseHandler = digitalWorkFlowExternalChannelResponseHandler;
    }

    /**
     * Callback nel caso di ritentativo a breve termine di invio PEC
     * @param iun IUN della notifica
     * @param recIndex id recipient
     * @param timelineId id timeline che ha generato la schedulazione
     */
    public void startScheduledRetryWorkflow(String iun, Integer recIndex, String timelineId) {
        log.debug("startScheduledRetryWorkflow - iun={} recIndex={} timelineId={}", iun, recIndex, timelineId);

        Optional<TimelineElementInternal> timelineElement = digitalWorkFlowUtils.getTimelineElement(iun, timelineId);
        if (timelineElement.isPresent() && timelineElement.get().getDetails() instanceof DigitalSendTimelineElementDetails) {
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            DigitalSendTimelineElementDetails originalSendDigitalProgressDetailsInt = (DigitalSendTimelineElementDetails) timelineElement.get().getDetails();


            if (checkIfEventIsStillValid(iun, recIndex, timelineElement.get())) {
                digitalWorkFlowHandler.sendDigitalNotificationAndScheduleTimeoutAction(notification,
                        originalSendDigitalProgressDetailsInt.getDigitalAddress(),
                        DigitalAddressInfoSentAttempt.builder()
                                .digitalAddress(originalSendDigitalProgressDetailsInt.getDigitalAddress())
                                .digitalAddressSource(originalSendDigitalProgressDetailsInt.getDigitalAddressSource())
                                .lastAttemptDate(timelineElement.get().getTimestamp())
                                .sentAttemptMade(originalSendDigitalProgressDetailsInt.getRetryNumber())
                                .build(), recIndex, true, timelineId);
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

        if (timelineElement.get().getDetails() instanceof DigitalSendTimelineElementDetails)
        {
            // dovrebbe sempre essere instanceof di questo tipo
            DigitalSendTimelineElementDetails sendDigitalProgressDetailsInt = (DigitalSendTimelineElementDetails) timelineElement.get().getDetails();
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
        // per capirlo, verifico se l'ultimo evento in ordine cronologico in timeline per recindex, appartiene a retrynumber e addresssource
        // è il senddigital o senddigitalprogress .
        // Se lo è, vuol dire che si può equiparare ad una risposta da ext-channel, che va trattata di conseguenza secondo configurazione.
        if (checkIfEventIsStillValid(iun, recIndex, timelineElement.get()))
        {
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
        else
        {
            // salvo cmq in timeline il fatto che ho deciso di non rischedulare i tentativi
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            DigitalAddressFeedback digitalAddressFeedback = DigitalAddressFeedback.builder()
                    .retryNumber(originalRetryNumber)
                    .eventTimestamp(Instant.now())
                    .digitalAddressSource(originalAddressSource)
                    .digitalAddress(originalAddressInfo)
                    .build();
            
            digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(notification,
                    EventCodeInt.DP10,
                    recIndex,
                    false,
                    null,
                    digitalAddressFeedback);

            log.error("elapsedExtChannelTimeout Last timelineevent doesn't match original timelineevent source and retrynumber, skipping more actions iun={} recIdx={}", iun, recIndex);
        }
    }

    /**
     * Ritorna TRUE se l'evento che ha schedulato il timer (che per ora è un evento di send di PEC)
     * è ancora "valido". Infatti potrei avere che l'utente ha VISUALIZZATO la notifica, o che nel frattempo è arrivata
     * da ext-channel (incredibilmente in ritardo ma fatalità proprio quando da fastidio :) ) il risultato dell'invio e quindi avere un FEEDBACK.
     * La logica sul re-invio di una PEC si basa sul fatto che il precedente (ri)tentativo sia ancora in corso, o sia fallito con esito di "retry".
     * Quindi mi devo aspettare che l'ULTIMO EVENTO IN TIMELINE IN ORDINE CRONOLOGICO sia appunto un evento di SEND_DIGITAL_PROGRESS
     * o SEND_DIGITAL_DOMICILE. Inoltre, per scrupolo controllo che sia relativo all'originale retry-number e all'originale source.
     *
     *
     * @param iun iun notifica
     * @param recIndex indice recipient
     * @param originalTimelineElement timelineId originale
     * @return TRUE se l'evento è valido e va gestito
     */
    private boolean checkIfEventIsStillValid(String iun, int recIndex, TimelineElementInternal originalTimelineElement){

        if (originalTimelineElement.getDetails() instanceof DigitalSendTimelineElementDetails) {
            DigitalSendTimelineElementDetails originalDigitalSendTimelineDetailsInt = (DigitalSendTimelineElementDetails) originalTimelineElement.getDetails();


            // devo controllare che il timeout scattato sia ancora rilevante.
            // per capirlo, verifico se l'ultimo evento in ordine cronologico in timeline per recindex, appartiene a retrynumber e addresssource
            // è il senddigital o senddigitalprogress .
            // Se lo è, vuol dire che si può equiparare ad una risposta da ext-channel, che va trattata di conseguenza secondo configurazione.
            TimelineElementInternal mostRecentElementInternal = digitalWorkFlowUtils.getMostRecentTimelineElement(iun, recIndex);
            log.info("checkIfEventIsStillValid mostRecentTimelineId for iun={} recIndex={} timelineId={}", iun, recIndex, mostRecentElementInternal.getElementId());

            Integer lastRetryNumber;
            DigitalAddressSourceInt lastAddressSource;

            // controllo gli eventi, e se sono di questi due tipi, mi interessa, sennò NO
            if ((mostRecentElementInternal.getCategory() == TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS
                    || mostRecentElementInternal.getCategory() == TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                    && mostRecentElementInternal.getDetails() instanceof DigitalSendTimelineElementDetails) {

                DigitalSendTimelineElementDetails mostrecentDigitalSendTimelineDetailsInt = (DigitalSendTimelineElementDetails) mostRecentElementInternal.getDetails();
                lastRetryNumber = mostrecentDigitalSendTimelineDetailsInt.getRetryNumber();
                lastAddressSource = mostrecentDigitalSendTimelineDetailsInt.getDigitalAddressSource();

                // per scrupolo, controllo anche che il retryNumber e l'addressSource siano gli stessi dell'originale.
                return originalDigitalSendTimelineDetailsInt.getRetryNumber().equals(lastRetryNumber)
                        && originalDigitalSendTimelineDetailsInt.getDigitalAddressSource() != null && lastAddressSource != null
                        && originalDigitalSendTimelineDetailsInt.getDigitalAddressSource().getValue().equals(lastAddressSource.getValue());
            }
        }
        return false;
    }


}
