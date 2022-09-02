package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.DigitalMessageReferenceInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class DigitalWorkFlowUtils {
    private final TimelineService timelineService;
    private final AddressBookService addressBookService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    
    public DigitalWorkFlowUtils(TimelineService timelineService, AddressBookService addressBookService, TimelineUtils timelineUtils, NotificationUtils notificationUtils) {
        this.timelineService = timelineService;
        this.addressBookService = addressBookService;
        this.timelineUtils = timelineUtils;
        this.notificationUtils = notificationUtils;
    }

    public DigitalAddressInfo getNextAddressInfo(String iun, Integer recIndex, DigitalAddressInfo lastAttemptMade) {
        log.debug("Start getNextAddressInfo - iun {} id {}", iun, recIndex);

        //Ottiene la source del prossimo indirizzo da utilizzare
        DigitalAddressSourceInt nextAddressSource = nextSource(lastAttemptMade.getDigitalAddressSource()) ;
        log.debug("nextAddressSource {}", nextAddressSource);

        DigitalAddressInfo nextAddressInfo = getNextAddressInfo(iun, recIndex, nextAddressSource);

        log.debug("GetNextAddressInfo completed - iun {} id {}", iun, recIndex);
        return nextAddressInfo;
    }

    private DigitalAddressInfo getNextAddressInfo(String iun, Integer recIndex, DigitalAddressSourceInt nextAddressSource) {
        Set<TimelineElementInternal> timeline = timelineService.getTimeline(iun);

        //Ottiene il numero di tentativi effettuati per tale indirizzo
        int nextSourceAttemptsMade = getAttemptsMadeForSource(recIndex, nextAddressSource, timeline);
        log.debug("AttemptsMade for source {} is {}", nextAddressSource, nextSourceAttemptsMade);

        Instant lastAttemptMadeForSource = null;

        if (nextSourceAttemptsMade > 0) {
            //Ottiene la data dell'ultimo tentativo effettuato per tale indirizzo
            lastAttemptMadeForSource = getLastAttemptDateForSource(recIndex, nextAddressSource, timeline);
            log.debug("lastAttemptMadeForSource for source {} is {}", nextAddressSource, lastAttemptMadeForSource);
        }

        return DigitalAddressInfo.builder()
                .digitalAddressSource(nextAddressSource)
                .sentAttemptMade(nextSourceAttemptsMade)
                .lastAttemptDate(lastAttemptMadeForSource)
                .build();

    }

    private Instant getLastAttemptDateForSource(Integer recIndex, DigitalAddressSourceInt nextAddressSource, Set<TimelineElementInternal> timeline) {
        Optional<GetAddressInfoDetailsInt> lastAddressAttemptOpt = timeline.stream()
                .filter(timelineElement -> filterTimelineForRecIndexAndSource(timelineElement, recIndex, nextAddressSource))
                .map(timelineElement -> (GetAddressInfoDetailsInt) timelineElement.getDetails())
                .max(Comparator.comparing(GetAddressInfoDetailsInt::getAttemptDate));

        if (lastAddressAttemptOpt.isPresent()) {
            log.debug("Get getLastAddressAttempt OK - id {}", recIndex);
            return lastAddressAttemptOpt.get().getAttemptDate();
        } else {
            log.error("Last address attempt not found - id {}", recIndex);
            throw new PnInternalException("Last address attempt not found - id" + recIndex);
        }

    }

    // Get attempts attempt made for source
    private int getAttemptsMadeForSource(Integer recIndex, DigitalAddressSourceInt nextAddressSource, Set<TimelineElementInternal> timeline) {
        return (int) timeline.stream()
                .filter(timelineElement -> filterTimelineForRecIndexAndSource(timelineElement, recIndex, nextAddressSource)).count();
    }

    private boolean filterTimelineForRecIndexAndSource(TimelineElementInternal el, Integer recIndex, DigitalAddressSourceInt source) {
        boolean availableAddressCategory = TimelineElementCategoryInt.GET_ADDRESS.equals(el.getCategory());
        
        if (availableAddressCategory) {
            GetAddressInfoDetailsInt getAddressInfo = (GetAddressInfoDetailsInt) el.getDetails();
            return recIndex.equals(getAddressInfo.getRecIndex()) && source.equals(getAddressInfo.getDigitalAddressSource());
        }
        return false;
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
        throw new PnInternalException("Specified addressSource " + addressSource + " does not exist - iun " + notification.getIun() + " id " + recipient.getTaxId());
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

    public ScheduleDigitalWorkflowDetailsInt getScheduleDigitalWorkflowTimelineElement(String iun, Integer recIndex) {
        String eventId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<ScheduleDigitalWorkflowDetailsInt> optTimeLineScheduleDigitalWorkflow = timelineService.getTimelineElementDetails(iun, eventId,
                ScheduleDigitalWorkflowDetailsInt.class);
        if (optTimeLineScheduleDigitalWorkflow.isPresent()) {
            return optTimeLineScheduleDigitalWorkflow.get();
        } else {
            log.error("ScheduleDigitalWorkflowTimelineElement element not exist - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("ScheduleDigitalWorkflowTimelineElement element not exist - iun " + iun + " eventId " + eventId);
        }
    }

    public TimelineElementInternal getSendDigitalDetailsTimelineElement(String iun, String eventId) {

        Optional<TimelineElementInternal> sendDigitalTimelineElement = timelineService.getTimelineElement(iun, eventId);
        
        if (sendDigitalTimelineElement.isPresent()) {
            return sendDigitalTimelineElement.get();
        } else {
            String error = String.format("SendDigital timeline element not exist -iun=%s requestId=%s", iun, eventId);
            log.error(error);
            throw new PnInternalException(error);
        }
    }

    public Optional<TimelineElementInternal> getTimelineElement(String iun, String eventId) {
        return timelineService.getTimelineElement(iun, eventId);
    }
    
    public void addScheduledDigitalWorkflowToTimeline(NotificationInt notification, Integer recIndex, DigitalAddressInfo lastAttemptMade) {
        addTimelineElement(
                timelineUtils.buildScheduleDigitalWorkflowTimeline(notification, recIndex, lastAttemptMade),
                notification
        );
    }

    public void addAvailabilitySourceToTimeline(Integer recIndex, NotificationInt notification, DigitalAddressSourceInt source, boolean isAvailable, int sentAttemptMade) {
        addTimelineElement(
                timelineUtils.buildAvailabilitySourceTimelineElement(recIndex, notification, source, isAvailable, sentAttemptMade),
                notification
        );
    }

    public void addDigitalFeedbackTimelineElement(NotificationInt notification, ResponseStatusInt status, List<String> errors,
                                                  SendDigitalDetailsInt sendDigitalDetails, DigitalMessageReferenceInt digitalMessageReference) {
        addTimelineElement(
                timelineUtils.buildDigitalFeedbackTimelineElement(notification, status, errors, sendDigitalDetails, digitalMessageReference),
                notification
        );
    }

    public void addDigitalDeliveringProgressTimelineElement(NotificationInt notification,
                                                            EventCodeInt eventCode,
                                                            boolean shouldRetry,
                                                            SendDigitalDetailsInt sendDigitalDetails,
                                                            DigitalMessageReferenceInt digitalMessageReference) {

        int progressIndex = computeNextTimelineProgressIndex(notification, sendDigitalDetails);

        addTimelineElement(
                timelineUtils.buildDigitalProgressFeedbackTimelineElement(notification, eventCode, shouldRetry, sendDigitalDetails, digitalMessageReference, progressIndex),
                notification
        );
    }


    private int computeNextTimelineProgressIndex(NotificationInt notification,
                                                  SendDigitalDetailsInt sendDigitalDetails){
        // per calcolare il prossimo progressIndex, devo necessariamente recuperare dal DB tutte le timeline relative a iun/recindex/source/tentativo
        String elementIdForSearch = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendDigitalDetails.getRecIndex())
                        .sentAttemptMade(sendDigitalDetails.getRetryNumber())
                        .source(sendDigitalDetails.getDigitalAddressSource())
                        .progressIndex(-1)  // passando -1 non verrà inserito nell'id timeline, permettendo la ricerca iniziaper
                        .build()
        );
        return this.timelineService.getTimelineByIunTimelineId(notification.getIun(), elementIdForSearch, false).size() + 1;
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

    public static DigitalAddressSourceInt nextSource(DigitalAddressSourceInt source) {
        switch (source) {
            case PLATFORM:
                return DigitalAddressSourceInt.SPECIAL;
            case SPECIAL:
                return DigitalAddressSourceInt.GENERAL;
            case GENERAL:
                return DigitalAddressSourceInt.PLATFORM;
            default:
                throw new PnInternalException(" BUG: add support to next for " + source.getClass() + "::" + source.name());
        }
    }
    
}
