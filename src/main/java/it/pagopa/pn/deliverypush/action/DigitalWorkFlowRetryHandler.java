package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelProgressEventCat;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalSendTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalProgressDetailsInt;
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
                        DigitalAddressInfo.builder()
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
            digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(notification,
                    EventCodeInt.DP10,
                    recIndex,
                    originalRetryNumber,
                    originalAddressInfo,
                    originalAddressSource,
                    false,
                    null,
                    Instant.now());

            log.info("elapsedExtChannelTimeout Last timelineevent doesn't match original timelineevent source and retrynumber, skipping more actions iun={} recIdx={}", iun, recIndex);
        }
    }

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

                return originalDigitalSendTimelineDetailsInt.getRetryNumber().equals(lastRetryNumber)
                        && originalDigitalSendTimelineDetailsInt.getDigitalAddressSource() != null && lastAddressSource != null
                        && originalDigitalSendTimelineDetailsInt.getDigitalAddressSource().getValue().equals(lastAddressSource.getValue());
            }
        }
        return false;
    }


}
