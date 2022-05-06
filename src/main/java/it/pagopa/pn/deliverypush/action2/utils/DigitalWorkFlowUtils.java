package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.external.AddressBook;
import it.pagopa.pn.deliverypush.external.AddressBookEntry;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class DigitalWorkFlowUtils {
    private final TimelineService timelineService;
    private final AddressBook addressBook;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    
    public DigitalWorkFlowUtils(TimelineService timelineService, AddressBook addressBook, TimelineUtils timelineUtils, NotificationUtils notificationUtils) {
        this.timelineService = timelineService;
        this.addressBook = addressBook;
        this.timelineUtils = timelineUtils;
        this.notificationUtils = notificationUtils;
    }

    public DigitalAddressInfo getNextAddressInfo(String iun, int recIndex, DigitalAddressInfo lastAttemptMade) {
        log.debug("Start getNextAddressInfo - iun {} id {}", iun, recIndex);

        //Ottiene la source del prossimo indirizzo da utilizzare
        DigitalAddressSource nextAddressSource = lastAttemptMade.getAddressSource().next();
        log.debug("nextAddressSource {}", nextAddressSource);

        DigitalAddressInfo nextAddressInfo = getNextAddressInfo(iun, recIndex, nextAddressSource);

        log.debug("GetNextAddressInfo completed - iun {} id {}", iun, recIndex);
        return nextAddressInfo;
    }

    private DigitalAddressInfo getNextAddressInfo(String iun, int recIndex, DigitalAddressSource nextAddressSource) {
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);

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
                .addressSource(nextAddressSource)
                .sentAttemptMade(nextSourceAttemptsMade)
                .lastAttemptDate(lastAttemptMadeForSource)
                .build();

    }

    private Instant getLastAttemptDateForSource(int recIndex, DigitalAddressSource nextAddressSource, Set<TimelineElement> timeline) {
        Optional<GetAddressInfo> lastAddressAttemptOpt = timeline.stream()
                .filter(timelineElement -> filterTimelineForRecIndexAndSource(timelineElement, recIndex, nextAddressSource))
                .map(timelineElement -> (GetAddressInfo) timelineElement.getDetails())
                .max(Comparator.comparing(GetAddressInfo::getAttemptDate));

        if (lastAddressAttemptOpt.isPresent()) {
            log.debug("Get getLastAddressAttempt OK - id {}", recIndex);
            return lastAddressAttemptOpt.get().getAttemptDate();
        } else {
            log.error("Last address attempt not found - id {}", recIndex);
            throw new PnInternalException("Last address attempt not found - id" + recIndex);
        }

    }

    // Get attempts number made for source
    private int getAttemptsMadeForSource(int recIndex, DigitalAddressSource nextAddressSource, Set<TimelineElement> timeline) {
        return (int) timeline.stream()
                .filter(timelineElement -> filterTimelineForRecIndexAndSource(timelineElement, recIndex, nextAddressSource)).count();
    }

    private boolean filterTimelineForRecIndexAndSource(TimelineElement el, int recIndex, DigitalAddressSource source) {
        boolean availableAddressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAddressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return recIndex == details.getRecIndex() && source.equals(details.getSource());
        }
        return false;
    }

    @Nullable
    public DigitalAddress getAddressFromSource(DigitalAddressSource addressSource, int recIndex, Notification notification) {
        log.info("GetAddressFromSource for source {} - iun {} id {}", addressSource, notification.getIun(), recIndex);
        NotificationRecipient recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        
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

    private void handleAddressSourceError(DigitalAddressSource addressSource, NotificationRecipient recipient, Notification notification) {
        log.error("Specified addressSource {} does not exist - iun {} id {}", addressSource, notification.getIun(), recipient.getTaxId());
        throw new PnInternalException("Specified addressSource " + addressSource + " does not exist - iun " + notification.getIun() + " id " + recipient.getTaxId());
    }

    private DigitalAddress retrievePlatformAddress(NotificationRecipient recipient, NotificationSender sender) {
        log.debug("RetrievePlatformAddress for recipient {} sender {}", recipient.getTaxId(), sender.getPaId());

        Optional<AddressBookEntry> addressBookEntryOpt = addressBook.getAddresses(recipient.getTaxId(), sender);

        if (addressBookEntryOpt.isPresent()) {
            log.debug("Retrive platformAddress ok for recipient {} sender {}", recipient.getTaxId(), sender.getPaId());
            DigitalAddress platformAddress = addressBookEntryOpt.get().getPlatformDigitalAddress();
            return platformAddress != null && platformAddress.getAddress() != null ? platformAddress : null;
        }
        log.info("Platform address is empty for recipient {} sender {}", recipient.getTaxId(), sender.getPaId());
        return null;
    }

    public ScheduleDigitalWorkflow getScheduleDigitalWorkflowTimelineElement(String iun, int recIndex) {
        String eventId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<ScheduleDigitalWorkflow> optTimeLineScheduleDigitalWorkflow = timelineService.getTimelineElement(iun, eventId,
                ScheduleDigitalWorkflow.class);
        if (optTimeLineScheduleDigitalWorkflow.isPresent()) {
            return optTimeLineScheduleDigitalWorkflow.get();
        } else {
            log.error("ScheduleDigitalWorkflowTimelineElement element not exist - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("ScheduleDigitalWorkflowTimelineElement element not exist - iun " + iun + " eventId " + eventId);
        }
    }

    public void addScheduledDigitalWorkflowToTimeline(String iun, int recIndex, DigitalAddressInfo lastAttemptMade) {
        addTimelineElement(timelineUtils.buildScheduleDigitalWorkflowTimeline(iun, recIndex, lastAttemptMade));
    }

    public void addAvailabilitySourceToTimeline(int recIndex, String iun, DigitalAddressSource source, boolean isAvailable, int sentAttemptMade) {
        addTimelineElement(timelineUtils.buildAvailabilitySourceTimelineElement(recIndex, iun, source, isAvailable, sentAttemptMade));
    }

    public void addDigitalFeedbackTimelineElement(ExtChannelResponse response, SendDigitalDetails sendDigitalDetails) {
        addTimelineElement(timelineUtils.buildDigitaFeedbackTimelineElement(response, sendDigitalDetails));
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }

}
