package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
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
        DigitalAddressSource nextAddressSource = nextSource(lastAttemptMade.getDigitalAddressSource()) ;
        log.debug("nextAddressSource {}", nextAddressSource);

        DigitalAddressInfo nextAddressInfo = getNextAddressInfo(iun, recIndex, nextAddressSource);

        log.debug("GetNextAddressInfo completed - iun {} id {}", iun, recIndex);
        return nextAddressInfo;
    }

    private DigitalAddressInfo getNextAddressInfo(String iun, Integer recIndex, DigitalAddressSource nextAddressSource) {
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

    private Instant getLastAttemptDateForSource(Integer recIndex, DigitalAddressSource nextAddressSource, Set<TimelineElementInternal> timeline) {
        Optional<GetAddressInfo> lastAddressAttemptOpt = timeline.stream()
                .filter(timelineElement -> filterTimelineForRecIndexAndSource(timelineElement, recIndex, nextAddressSource))
                .map(timelineElement -> SmartMapper.mapToClass(timelineElement.getDetails(), GetAddressInfo.class))
                .max(Comparator.comparing(GetAddressInfo::getAttemptDate));

        if (lastAddressAttemptOpt.isPresent()) {
            log.debug("Get getLastAddressAttempt OK - id {}", recIndex);
            return lastAddressAttemptOpt.get().getAttemptDate();
        } else {
            log.error("Last address attempt not found - id {}", recIndex);
            throw new PnInternalException("Last address attempt not found - id" + recIndex);
        }

    }

    // Get attempts attempt made for source
    private int getAttemptsMadeForSource(Integer recIndex, DigitalAddressSource nextAddressSource, Set<TimelineElementInternal> timeline) {
        return (int) timeline.stream()
                .filter(timelineElement -> filterTimelineForRecIndexAndSource(timelineElement, recIndex, nextAddressSource)).count();
    }

    private boolean filterTimelineForRecIndexAndSource(TimelineElementInternal el, Integer recIndex, DigitalAddressSource source) {
        boolean availableAddressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        
        if (availableAddressCategory) {
            GetAddressInfo getAddressInfo = SmartMapper.mapToClass(el.getDetails(), GetAddressInfo.class);
            return recIndex.equals(getAddressInfo.getRecIndex()) && source.equals(getAddressInfo.getDigitalAddressSource());
        }
        return false;
    }

    @Nullable
    public LegalDigitalAddressInt getAddressFromSource(DigitalAddressSource addressSource, Integer recIndex, NotificationInt notification) {
        log.info("GetAddressFromSource for source {} - iun {} id {}", addressSource, notification.getIun(), recIndex);
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        
        if (addressSource != null) {
            switch (addressSource) {
                case PLATFORM:
                    return retrievePlatformAddress(recipient, notification.getSender());
                case SPECIAL:
                    log.debug("Return digital domicile - iun {} id {}", notification.getIun(), recipient.getTaxId());
                    return recipient.getDigitalDomicile();
                default:
                    handleAddressSourceError(addressSource, recipient, notification);
            }
        } else {
            handleAddressSourceError(null, recipient, notification);
        }
        return null;
    }

    private void handleAddressSourceError(DigitalAddressSource addressSource, NotificationRecipientInt recipient, NotificationInt notification) {
        log.error("Specified addressSource {} does not exist - iun {} id {}", addressSource, notification.getIun(), recipient.getTaxId());
        throw new PnInternalException("Specified addressSource " + addressSource + " does not exist - iun " + notification.getIun() + " id " + recipient.getTaxId());
    }

    private LegalDigitalAddressInt retrievePlatformAddress(NotificationRecipientInt recipient, NotificationSenderInt sender) {
        log.debug("RetrievePlatformAddress for recipient {} sender {}", recipient.getTaxId(), sender.getPaId());

        Optional<LegalDigitalAddressInt> digitalAddressOpt = addressBookService.getPlatformAddresses(recipient.getTaxId(), sender.getPaId());

        if (digitalAddressOpt.isPresent()) {
            log.debug("Retrive platformAddress ok for recipient {} sender {}", recipient.getTaxId(), sender.getPaId());
            return digitalAddressOpt.get();
        }
        log.info("Platform address is empty for recipient {} sender {}", recipient.getTaxId(), sender.getPaId());
        return null;
    }

    public ScheduleDigitalWorkflow getScheduleDigitalWorkflowTimelineElement(String iun, Integer recIndex) {
        String eventId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<ScheduleDigitalWorkflow> optTimeLineScheduleDigitalWorkflow = timelineService.getTimelineElementDetails(iun, eventId,
                ScheduleDigitalWorkflow.class);
        if (optTimeLineScheduleDigitalWorkflow.isPresent()) {
            return optTimeLineScheduleDigitalWorkflow.get();
        } else {
            log.error("ScheduleDigitalWorkflowTimelineElement element not exist - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("ScheduleDigitalWorkflowTimelineElement element not exist - iun " + iun + " eventId " + eventId);
        }
    }

    public void addScheduledDigitalWorkflowToTimeline(String iun, Integer recIndex, DigitalAddressInfo lastAttemptMade) {
        addTimelineElement(timelineUtils.buildScheduleDigitalWorkflowTimeline(iun, recIndex, lastAttemptMade));
    }

    public void addAvailabilitySourceToTimeline(Integer recIndex, String iun, DigitalAddressSource source, boolean isAvailable, int sentAttemptMade) {
        addTimelineElement(timelineUtils.buildAvailabilitySourceTimelineElement(recIndex, iun, source, isAvailable, sentAttemptMade));
    }

    public void addDigitalFeedbackTimelineElement(String iun, ResponseStatus status, List<String> errors, SendDigitalDetails sendDigitalDetails) {
        addTimelineElement(timelineUtils.buildDigitalFeedbackTimelineElement(iun, status, errors, sendDigitalDetails));
    }

    private void addTimelineElement(TimelineElementInternal element) {
        timelineService.addTimelineElement(element);
    }

    public static DigitalAddressSource nextSource(DigitalAddressSource source) {
        switch (source) {
            case PLATFORM:
                return DigitalAddressSource.SPECIAL;
            case SPECIAL:
                return DigitalAddressSource.GENERAL;
            case GENERAL:
                return DigitalAddressSource.PLATFORM;
            default:
                throw new PnInternalException(" BUG: add support to next for " + source.getClass() + "::" + source.name());
        }
    }
}
