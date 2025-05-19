package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.DigitalMessageReferenceInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Component
@AllArgsConstructor
@Slf4j
public class DigitalWorkFlowUtils {
    private final TimelineService timelineService;
    private final AddressBookService addressBookService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;



    public DigitalAddressInfoSentAttempt getNextAddressInfo(String iun, Integer recIndex, DigitalAddressInfoSentAttempt lastAttemptMade, boolean isPfWorkflowEnabled) {
        log.debug("Start getNextAddressInfo - iun {} id {}", iun, recIndex);

        //Ottiene la source del prossimo indirizzo da utilizzare
        DigitalAddressSourceInt nextAddressSource = nextSource(lastAttemptMade.getDigitalAddressSource(), isPfWorkflowEnabled);
        log.debug("nextAddressSource {}", nextAddressSource);

        DigitalAddressInfoSentAttempt nextAddressInfo = getNextAddressInfo(iun, recIndex, nextAddressSource);

        log.debug("GetNextAddressInfo completed - iun {} id {}", iun, recIndex);
        return nextAddressInfo;
    }

    private DigitalAddressInfoSentAttempt getNextAddressInfo(String iun, Integer recIndex, DigitalAddressSourceInt nextAddressSource) {
        Set<TimelineElementInternal> timeline = timelineService.getTimeline(iun, true);

        //Ottiene il numero di tentativi effettuati per tale indirizzo
        int nextSourceAttemptsMade = getAttemptsMadeForSource(recIndex, nextAddressSource, timeline);
        log.debug("AttemptsMade for source {} is {}", nextAddressSource, nextSourceAttemptsMade);

        Instant lastAttemptMadeForSource = null;

        if (nextSourceAttemptsMade > 0) {
            //Ottiene la data dell'ultimo tentativo effettuato per tale indirizzo
            lastAttemptMadeForSource = getLastAttemptDateForSource(recIndex, nextAddressSource, timeline);
            log.debug("lastAttemptMadeForSource for source {} is {}", nextAddressSource, lastAttemptMadeForSource);
        }

        return DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(nextAddressSource)
                .sentAttemptMade(nextSourceAttemptsMade)
                .lastAttemptDate(lastAttemptMadeForSource)
                .build();

    }

    /**
     * Devo recuperare l'evento più recente per un certo source.
     * L'evento potrebbe essere la GET_ADDRESS (se poi non ha dato luogo ad un indirizzo da usare)
     * o una DIGITAL_FEEDBACK se invece è stato effettuato un tentativo di invio e ho un esito
     *
     * @param recIndex indice recipient
     * @param nextAddressSource sorgente su cui cercare
     * @param timeline lista timeline
     * @return Data dell'ultimo evento per il source passato
     */
    private Instant getLastAttemptDateForSource(Integer recIndex, DigitalAddressSourceInt nextAddressSource, Set<TimelineElementInternal> timeline) {
        // Cerco l'evento di get_address più recente, qui si guarda l'attemptDate
        Optional<GetAddressInfoDetailsInt> lastAddressAttemptOpt = timeline.stream()
                .filter(timelineElement -> filterTimelineForRecIndexAndSource(timelineElement, TimelineElementCategoryInt.GET_ADDRESS, recIndex, nextAddressSource))
                .map(timelineElement -> (GetAddressInfoDetailsInt) timelineElement.getDetails())
                .max(Comparator.comparing(GetAddressInfoDetailsInt::getAttemptDate));

        // Cerco l'evento di digital_feedback più recente, qui si guarda la notificationDate
        Optional<SendDigitalFeedbackDetailsInt> lastAddressResultOpt = timeline.stream()
                .filter(timelineElement -> filterTimelineForRecIndexAndSource(timelineElement, TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, recIndex, nextAddressSource))
                .map(timelineElement -> (SendDigitalFeedbackDetailsInt) timelineElement.getDetails())
                .max(Comparator.comparing(SendDigitalFeedbackDetailsInt::getNotificationDate));

        // risolvo prendendo il più recente tra i 2 eventi
        Instant lastAttemptDate = lastAddressAttemptOpt.isPresent()?lastAddressAttemptOpt.get().getAttemptDate() : Instant.EPOCH;
        lastAttemptDate = lastAddressResultOpt.isPresent() && lastAttemptDate.isBefore(lastAddressResultOpt.get().getNotificationDate())?lastAddressResultOpt.get().getNotificationDate() : lastAttemptDate;

        if (lastAttemptDate.isAfter(Instant.EPOCH)) {
            log.debug("Get getLastAttemptDateForSource OK - id {}", recIndex);
            return lastAttemptDate;
        } else {
            log.error("Last address attempt not found - id {}", recIndex);
            throw new PnInternalException("Last address attempt not found - id" + recIndex, ERROR_CODE_DELIVERYPUSH_LASTADDRESSATTEMPTNOTFOUND);
        }

    }

    // Get attempts attempt made for source
    private int getAttemptsMadeForSource(Integer recIndex, DigitalAddressSourceInt nextAddressSource, Set<TimelineElementInternal> timeline) {
        return (int) timeline.stream()
                .filter(timelineElement -> filterTimelineForRecIndexAndSource(timelineElement, TimelineElementCategoryInt.GET_ADDRESS, recIndex, nextAddressSource)).count();
    }



    private boolean filterTimelineForRecIndexAndSource(
            TimelineElementInternal el, TimelineElementCategoryInt filterByCategory, Integer recIndex, DigitalAddressSourceInt source) {
        boolean availableAddressCategory = filterByCategory.equals(el.getCategory());
        TimelineElementDetailsInt detailsInt = el.getDetails();

        return availableAddressCategory
            && detailsInt instanceof DigitalAddressSourceRelatedTimelineElement digitalAddressSourceRelatedTimelineElement
            && recIndex.equals(digitalAddressSourceRelatedTimelineElement.getRecIndex())
            && source.equals(digitalAddressSourceRelatedTimelineElement.getDigitalAddressSource());
    }

    @Nullable
    public LegalDigitalAddressInt getAddressFromSource(DigitalAddressSourceInt addressSource, Integer recIndex, NotificationInt notification) {
        log.info("GetAddressFromSource for source {} - iun {} id {}", addressSource, notification.getIun(), recIndex);
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        if (addressSource != null) {
            switch (addressSource) {
                case PLATFORM:
                    return retrievePlatformAddress(recipient, notification.getSender());
                case SPECIAL:
                    log.debug("Return digital domicile - iun {} id {}", notification.getIun(), recIndex);
                    return recipient.getDigitalDomicile();
                default:
                    handleAddressSourceError(addressSource, recipient, notification);
            }
        } else {
            handleAddressSourceError(null, recipient, notification);
        }
        return null;
    }

    private void handleAddressSourceError(DigitalAddressSourceInt addressSource, NotificationRecipientInt recipient, NotificationInt notification) {
        log.error("Specified addressSource {} does not exist - iun {} id {}", addressSource, notification.getIun(), recipient.getTaxId());
        throw new PnInternalException("Specified addressSource " + addressSource + " does not exist - iun " + notification.getIun() + " id " + recipient.getTaxId(), ERROR_CODE_DELIVERYPUSH_INVALIDADDRESSSOURCE);
    }

    private LegalDigitalAddressInt retrievePlatformAddress(NotificationRecipientInt recipient, NotificationSenderInt sender) {
        log.debug("RetrievePlatformAddress for sender {}", sender.getPaId());

        Optional<LegalDigitalAddressInt> digitalAddressOpt = addressBookService.getPlatformAddresses(recipient.getInternalId(), sender.getPaId());

        if (digitalAddressOpt.isPresent()) {
            log.debug("Retrive platformAddress ok for recipient {} sender {}", recipient.getTaxId(), sender.getPaId());
            return digitalAddressOpt.get();
        }
        log.info("Platform address is empty for sender {}", sender.getPaId());
        return null;
    }

    public ScheduleDigitalWorkflowDetailsInt getScheduleDigitalWorkflowTimelineElement(String iun, String timelineId) {

        Optional<ScheduleDigitalWorkflowDetailsInt> optTimeLineScheduleDigitalWorkflow = timelineService.getTimelineElementDetails(iun, timelineId,
                ScheduleDigitalWorkflowDetailsInt.class);
        if (optTimeLineScheduleDigitalWorkflow.isPresent()) {
            return optTimeLineScheduleDigitalWorkflow.get();
        } else {
            log.error("ScheduleDigitalWorkflowTimelineElement element not exist - iun {} eventId {}", iun, timelineId);
            throw new PnInternalException("ScheduleDigitalWorkflowTimelineElement element not exist - iun " + iun + " eventId " + timelineId, ERROR_CODE_DELIVERYPUSH_SCHEDULEDDIGITALTIMELINEEVENTNOTFOUND);
        }
    }

    /**
     * Ritorna l'evento più recente per iun/recipient
     * @param iun IUN notifica
     * @param recIndex indice recipient
     * @return Evento timeline più recente
     */
  /*  public TimelineElementInternal getMostRecentTimelineElement(String iun, Integer recIndex) {
        Set<TimelineElementInternal> timelineElementInternals = timelineService.getTimeline(iun, false);
        Optional<TimelineElementInternal> timelineElementInternal = timelineElementInternals.stream()
                .filter(timeElementInternal -> !timeElementInternal.getCategory().equals(SEND_COURTESY_MESSAGE))    //potrebbe essere arrivato un evento di courtesy per esempio l'attivazione di IO che va ignorato. Tutti gli altri eventi vanno considerati invece.
                .max(Comparator.comparing(TimelineElementInternal::getTimestamp));

        if (timelineElementInternal.isPresent()) {
            return timelineElementInternal.get();
        } else {
            log.error("TimelineElementInternal element for recindex not exist - iun {} recIndex {}", iun, recIndex);
            throw new PnInternalException("TimelineElementInternal element for recindex not exist - iun " + iun + " recIndex " + recIndex, ERROR_CODE_DELIVERYPUSH_TIMELINEEVENTNOTFOUND);
        }
    }*/
    
    public Optional<TimelineElementInternal> getSendDigitalFeedbackFromSourceTimeline(String iun, DigitalSendTimelineElementDetails originalDigitalSendTimelineDetailsInt) {
        String elementId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(originalDigitalSendTimelineDetailsInt.getRecIndex())
                        .sentAttemptMade(originalDigitalSendTimelineDetailsInt.getRetryNumber())
                        .source(originalDigitalSendTimelineDetailsInt.getDigitalAddressSource())
                        .isFirstSendRetry(originalDigitalSendTimelineDetailsInt.getIsFirstSendRetry())
                        .build()
        );

        return timelineService.getTimelineElement(iun, elementId);
    }

    public Optional<TimelineElementInternal> getTimelineElement(String iun, String eventId) {
        return timelineService.getTimelineElement(iun, eventId);
    }

    public String addScheduledDigitalWorkflowToTimeline(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt lastAttemptMade, Instant schedulingDate) {
        return addTimelineElement(
                timelineUtils.buildScheduleDigitalWorkflowTimeline(notification, recIndex, lastAttemptMade, schedulingDate),
                notification
        );
    }

    public void addAvailabilitySourceToTimeline(Integer recIndex, NotificationInt notification, DigitalAddressSourceInt source, boolean isAvailable, int sentAttemptMade) {
        addTimelineElement(
                timelineUtils.buildAvailabilitySourceTimelineElement(recIndex, notification, source, isAvailable, sentAttemptMade),
                notification
        );
    }


    public String addPrepareSendToTimeline(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt lastAttemptMade, DigitalAddressInfoSentAttempt nextAddressInfo, String sourceTimelineId) {

        // il metodo si preoccupa di salvare in timeline le info del lastattemptmade e del nextaddressinfo, verranno recuperate poi
        return addTimelineElement(
                timelineUtils.buildPrepareDigitalNotificationTimelineElement(notification, recIndex, lastAttemptMade.getDigitalAddress(), lastAttemptMade.getDigitalAddressSource(), lastAttemptMade.getSentAttemptMade(), lastAttemptMade.getLastAttemptDate(),
                        nextAddressInfo.getDigitalAddressSource(), nextAddressInfo.getLastAttemptDate(), nextAddressInfo.getSentAttemptMade(), sourceTimelineId),
                notification
        );
    }


    public PrepareDigitalDetailsInt getPrepareSendDigitalWorkflowTimelineElement(String iun, String timelineId) {

        Optional<PrepareDigitalDetailsInt> optTimeLineScheduleDigitalWorkflow = timelineService.getTimelineElementDetails(iun, timelineId,
                PrepareDigitalDetailsInt.class);
        if (optTimeLineScheduleDigitalWorkflow.isPresent()) {
            return optTimeLineScheduleDigitalWorkflow.get();
        } else {
            log.error("getPrepareSendDigitalWorkflowTimelineElement element not exist - iun {} eventId {}", iun, timelineId);
            throw new PnInternalException("getPrepareSendDigitalWorkflowTimelineElement element not exist - iun " + iun + " eventId " + timelineId, ERROR_CODE_DELIVERYPUSH_SCHEDULEDPREPARETIMELINEEVENTNOTFOUND);
        }
    }

    public DigitalAddressInfoSentAttempt getDigitalAddressInfoSentAttemptLastAttemptMadeFromPrepare(PrepareDigitalDetailsInt prepareDigitalDetailsInt) {
        // ricostruisco il lastAttemptMade
        return DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(prepareDigitalDetailsInt.getAttemptDate())
                .sentAttemptMade(prepareDigitalDetailsInt.getRetryNumber())
                .digitalAddress(prepareDigitalDetailsInt.getDigitalAddress())
                .digitalAddressSource(prepareDigitalDetailsInt.getDigitalAddressSource())
                .build();
    }

    public DigitalAddressInfoSentAttempt getDigitalAddressInfoSentAttemptNextAddressInfoFromPrepare(PrepareDigitalDetailsInt prepareDigitalDetailsInt) {
        // ricostruisco il nextAddressInfo
        // NB: il digitalAddress non è popolato
        return DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(prepareDigitalDetailsInt.getNextLastAttemptMadeForSource())
                .sentAttemptMade(prepareDigitalDetailsInt.getNextSourceAttemptsMade())
                .digitalAddressSource(prepareDigitalDetailsInt.getNextDigitalAddressSource())
                .build();
    }


    public String addDigitalFeedbackTimelineElement(
                                                  String digitalDomicileTimeLineId,
                                                  NotificationInt notification,
                                                  ResponseStatusInt status,
                                                  int recIndex,
                                                  ExtChannelDigitalSentResponseInt extChannelDigitalSentResponseInt,
                                                  SendInformation digitalAddressInfo,
                                                  Boolean isFirstSentRetry
                                                  ) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildDigitalFeedbackTimelineElement(
                digitalDomicileTimeLineId,notification, status, recIndex, extChannelDigitalSentResponseInt, digitalAddressInfo, isFirstSentRetry
        );
        addTimelineElement(timelineElementInternal, notification);

        return timelineElementInternal.getElementId();
    }

    public void addDigitalDeliveringProgressTimelineElement(NotificationInt notification,
                                                            EventCodeInt eventCode,
                                                            int recIndex,
                                                            boolean shouldRetry,
                                                            DigitalMessageReferenceInt digitalMessageReference,
                                                            SendInformation digitalAddressFeedback) {

        int progressIndex = getPreviousTimelineProgress(notification, recIndex, digitalAddressFeedback.getRetryNumber(),
                digitalAddressFeedback.getIsFirstSendRetry(), digitalAddressFeedback.getDigitalAddressSource()).size() + 1;

        addTimelineElement(
                timelineUtils.buildDigitalProgressFeedbackTimelineElement(
                        notification,
                        recIndex,
                        eventCode,
                        shouldRetry,
                        digitalMessageReference,
                        progressIndex,
                        digitalAddressFeedback
                ),
                notification
        );
    }


    public Set<TimelineElementInternal> getPreviousTimelineProgress(NotificationInt notification,
                                                  int recIndex, int attemptMade, Boolean isFirstSentRetry, DigitalAddressSourceInt digitalAddressSourceInt){
        // per calcolare il prossimo progressIndex, devo necessariamente recuperare dal DB tutte le timeline relative a iun/recindex/source/tentativo
        String elementIdForSearch = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(attemptMade)
                        .source(digitalAddressSourceInt)
                        .isFirstSendRetry(isFirstSentRetry)
                        .progressIndex(-1)  // passando -1 non verrà inserito nell'id timeline, permettendo la ricerca iniziaper
                        .build()
        );
        return this.timelineService.getTimelineByIunTimelineId(notification.getIun(), elementIdForSearch, false);
    }

    private String addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        String timelineId = element.getElementId();
        timelineService.addTimelineElement(element, notification);
        return timelineId;
    }

    public static DigitalAddressSourceInt nextSource(DigitalAddressSourceInt source, boolean isPfNewWorkflowEnabled) {
        if (isPfNewWorkflowEnabled) {
            return switch (source) {
                case GENERAL -> DigitalAddressSourceInt.SPECIAL;
                case SPECIAL -> DigitalAddressSourceInt.PLATFORM;
                case PLATFORM -> DigitalAddressSourceInt.GENERAL;
                default -> throw new PnInternalException(" BUG: add support to next for " + source.getClass() + "::" + source.name(), ERROR_CODE_DELIVERYPUSH_INVALIDADDRESSSOURCE);
            };
        } else {
            return switch (source) {
                case PLATFORM -> DigitalAddressSourceInt.SPECIAL;
                case SPECIAL -> DigitalAddressSourceInt.GENERAL;
                case GENERAL -> DigitalAddressSourceInt.PLATFORM;
                default -> throw new PnInternalException(" BUG: add support to next for " + source.getClass() + "::" + source.name(), ERROR_CODE_DELIVERYPUSH_INVALIDADDRESSSOURCE);
            };
        }
    }
}
