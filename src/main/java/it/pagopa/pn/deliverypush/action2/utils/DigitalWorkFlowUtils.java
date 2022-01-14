package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class DigitalWorkFlowUtils {
    private final TimelineService timelineService;
    private final AddressBook addressBook;
    private final TimelineUtils timelineUtils;

    public DigitalWorkFlowUtils(TimelineService timelineService, AddressBook addressBook, TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.addressBook = addressBook;
        this.timelineUtils = timelineUtils;
    }

    public DigitalAddressInfo getNextAddressInfo(String iun, String taxId, DigitalAddressInfo lastAttemptMade) {
        log.debug("Start getNextAddressInfo - iun {} id {}", iun, taxId);

        //Ottiene la source del prossimo indirizzo da utilizzare
        DigitalAddressSource nextAddressSource = lastAttemptMade.getAddressSource().next();
        log.debug("nextAddressSource {}", nextAddressSource);

        DigitalAddressInfo nextAddressInfo = getNextAddressInfo(iun, taxId, nextAddressSource);

        log.debug("GetNextAddressInfo completed - iun {} id {}", iun, taxId);
        return nextAddressInfo;
    }

    private DigitalAddressInfo getNextAddressInfo(String iun, String taxId, DigitalAddressSource nextAddressSource) {
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);

        //Ottiene il numero di tentativi effettuati per tale indirizzo
        int nextSourceAttemptsMade = getAttemptsMadeForSource(taxId, nextAddressSource, timeline);
        log.debug("AttemptsMade for source {} is {}", nextAddressSource, nextSourceAttemptsMade);

        Instant lastAttemptMadeForSource = null;

        if (nextSourceAttemptsMade > 0) {
            //Ottiene la data dell'ultimo tentativo effettuato per tale indirizzo
            lastAttemptMadeForSource = getLastAttemptDateForSource(taxId, nextAddressSource, timeline);
            log.debug("lastAttemptMadeForSource for source {} is {}", nextAddressSource, lastAttemptMadeForSource);
        }

        return DigitalAddressInfo.builder()
                .addressSource(nextAddressSource)
                .sentAttemptMade(nextSourceAttemptsMade)
                .lastAttemptDate(lastAttemptMadeForSource)
                .build();

    }

    //Ottiene l'ultimo indirizzo dalla timeline. I tentavi sono sempre presenti in timeline, sia nel caso in cui l'indirizzo sia presente sia nel caso in cui non lo sia
    private GetAddressInfo getLastAddressAttempt(String iun, String taxId, Set<TimelineElement> timeline) {
        log.debug("GetLastAddressAttempt - iun {} id {}", iun, taxId);

        Optional<GetAddressInfo> lastAddressAttemptOpt = timeline.stream()
                .filter(timelineElement -> checkGetAddressCategoryAndTaxId(timelineElement, taxId))
                .map(timelineElement -> (GetAddressInfo) timelineElement.getDetails())
                .max(Comparator.comparing(GetAddressInfo::getAttemptDate));

        if (lastAddressAttemptOpt.isPresent()) {
            log.debug("Get getLastAddressAttempt OK - iun {} id {}", iun, taxId);
            return lastAddressAttemptOpt.get();
        } else {
            log.error("Last address attempt not found - iun {} id {}", iun, taxId);
            throw new PnInternalException("Last address attempt not found - iun " + iun + " id" + taxId);
        }
    }

    private boolean checkGetAddressCategoryAndTaxId(TimelineElement el, String taxId) {
        boolean availableAddressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAddressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    private Instant getLastAttemptDateForSource(String taxId, DigitalAddressSource nextAddressSource, Set<TimelineElement> timeline) {
        Optional<GetAddressInfo> lastAddressAttemptOpt = timeline.stream()
                .filter(timelineElement -> filterTimelineForTaxIdAndSource(timelineElement, taxId, nextAddressSource))
                .map(timelineElement -> (GetAddressInfo) timelineElement.getDetails())
                .max(Comparator.comparing(GetAddressInfo::getAttemptDate));

        if (lastAddressAttemptOpt.isPresent()) {
            log.debug("Get getLastAddressAttempt OK - id {}", taxId);
            return lastAddressAttemptOpt.get().getAttemptDate();
        } else {
            log.error("Last address attempt not found - id {}", taxId);
            throw new PnInternalException("Last address attempt not found - id" + taxId);
        }

    }

    // Get attempts number made for source
    private int getAttemptsMadeForSource(String taxId, DigitalAddressSource nextAddressSource, Set<TimelineElement> timeline) {
        return (int) timeline.stream()
                .filter(timelineElement -> filterTimelineForTaxIdAndSource(timelineElement, taxId, nextAddressSource)).count();
    }

    private boolean filterTimelineForTaxIdAndSource(TimelineElement el, String taxId, DigitalAddressSource source) {
        boolean availableAddressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAddressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId()) && source.equals(details.getSource());
        }
        return false;
    }

    @Nullable
    public DigitalAddress getAddressFromSource(DigitalAddressSource addressSource, NotificationRecipient recipient, Notification notification) {
        log.info("GetAddressFromSource for source {} - iun {} id {}", addressSource, notification.getIun(), recipient.getTaxId());
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
            handleAddressSourceError(addressSource, recipient, notification);
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

            DigitalAddresses digitalAddresses = addressBookEntryOpt.get().getDigitalAddresses(); //TODO Valutare se far ritornare un solo indirizzo all'addressbook e non una lista
            DigitalAddress platformAddress = digitalAddresses.getPlatform();
            return platformAddress != null && platformAddress.getAddress() != null ? platformAddress : null;
        }
        log.info("Platform address is empty for recipient {} sender {}", recipient.getTaxId(), sender.getPaId());
        return null;
    }

    public ScheduleDigitalWorkflow getScheduleDigitalWorkflowTimelineElement(String iun, String taxId) {
        String eventId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
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

    public void addScheduledDigitalWorkflowToTimeline(String iun, String taxId, DigitalAddressInfo lastAttemptMade) {
        addTimelineElement(timelineUtils.buildScheduledDigitalWorkflowTimeline(iun, taxId, lastAttemptMade));
    }

    public void addAvailabilitySourceToTimeline(String taxId, String iun, DigitalAddressSource source, boolean isAvailable, int sentAttemptMade) {
        addTimelineElement(timelineUtils.buildAvailabilitySourceTimelineElement(taxId, iun, source, isAvailable, sentAttemptMade));
    }

    public void addDigitalFailureAttemptTimelineElement(ExtChannelResponse response) {
        addTimelineElement(timelineUtils.buildDigitalFailureAttemptTimelineElement(response));
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }

}
