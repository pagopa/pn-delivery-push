package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.AttemptAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.timeline.GetAddressInfo;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class DigitalWorkFlowUtils {
    private final TimelineService timelineService;
    private final AddressBook addressBook;

    public DigitalWorkFlowUtils(TimelineService timelineService, AddressBook addressBook) {
        this.timelineService = timelineService;
        this.addressBook = addressBook;
    }

    public AttemptAddressInfo getNextAddressInfo(String iun, String taxId) {
        log.info("Start getNextAddressInfo for iun {} id {}", iun, taxId);

        //TODO Da rivedere i metodi utilizzati per filtrare ecc
        AttemptAddressInfo attemptAddressInfo;
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);

        //Viene ottenuto l'ultimo indirizzo utilizzato
        GetAddressInfo lastAddressAttempt = getLastAddressAttempt(iun, taxId, timeline);
        log.debug("Get last address attempt with source {}", lastAddressAttempt.getSource());

        //Ottiene la source del prossimo indirizzo da utilizzare
        DigitalAddressSource nextAddressSource = getNextAddressSource(lastAddressAttempt.getSource());
        log.debug("nextAddressSource {}", nextAddressSource);

        //Ottiene i tentativi effettuati per tale indirizzo
        int attemptsMade = getAttemptsMadeForSource(taxId, timeline, nextAddressSource);
        log.debug("AttemptsMade for source {} is {}", nextAddressSource, attemptsMade);

        attemptAddressInfo = AttemptAddressInfo.builder()
                .addressSource(nextAddressSource)
                .sentAttemptMade(attemptsMade)
                .lastAttemptDate(lastAddressAttempt.getAttemptDate())
                .build();

        log.info("GetNextAddressInfo completed for iun {} id {}", iun, taxId);

        return attemptAddressInfo;
    }

    //Get last tried source address from timeline. Attempt for source is ever added in timeline (both in case the address is available and if it's not available)
    private GetAddressInfo getLastAddressAttempt(String iun, String taxId, Set<TimelineElement> timeline) {
        log.debug("GetLastAddressAttempt for iun {} id {}", iun, taxId);

        Optional<GetAddressInfo> lastAddressAttemptOpt = timeline.stream()
                .filter(timelineElement -> filterLastAttemptDateInTimeline(timelineElement, taxId))
                .map(timelineElement -> (GetAddressInfo) timelineElement.getDetails()).min(Comparator.comparing(GetAddressInfo::getAttemptDate));

        if (lastAddressAttemptOpt.isPresent()) {
            log.debug("Get getLastAddressAttempt OK for iun {} id {}", iun, taxId);
            return lastAddressAttemptOpt.get();
        } else {
            log.error("Last address attempt not found for iun {} id {}", iun, taxId);
            throw new PnInternalException("Last address attempt not found for iun " + iun + " id" + taxId);
        }
    }

    private boolean filterLastAttemptDateInTimeline(TimelineElement el, String taxId) {
        boolean availableAddressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAddressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    // Get attempts number made for passed source
    private int getAttemptsMadeForSource(String taxId, Set<TimelineElement> timeline, DigitalAddressSource nextAddressSource) {
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

    /**
     * Get next address source from passed source in this order: PLATFORM, GENERAL, SPECIAL
     */
    public DigitalAddressSource getNextAddressSource(DigitalAddressSource addressSource) {
        log.debug("GetNextAddressSource for source {}", addressSource);

        if (addressSource != null) {
            switch (addressSource) {
                case PLATFORM:
                    return DigitalAddressSource.GENERAL;
                case GENERAL:
                    return DigitalAddressSource.SPECIAL;
                case SPECIAL:
                    return DigitalAddressSource.PLATFORM;
                default:
                    handleAddressSourceError(addressSource);
            }
        } else {
            handleAddressSourceError(addressSource);
        }
        return null;
    }

    private void handleAddressSourceError(DigitalAddressSource addressSource) {
        log.error("Address source {} is not valid", addressSource);
        throw new PnInternalException("Address source " + addressSource + " is not valid");
    }

    @Nullable
    public DigitalAddress getAddressFromSource(DigitalAddressSource addressSource, NotificationRecipient recipient, Notification notification) {
        log.info("GetAddressFromSource for source {} iun {} id {}", addressSource, notification.getIun(), recipient.getTaxId());
        if (addressSource != null) {
            switch (addressSource) {
                case PLATFORM:
                    return retrievePlatformAddress(recipient, notification.getSender());
                case SPECIAL:
                    log.debug("Return digital domicile for iun {} id {}", notification.getIun(), recipient.getTaxId());
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
        log.error("Specified addressSource {} does not exist for iun {} id {}", addressSource, notification.getIun(), recipient.getTaxId());
        throw new PnInternalException("Specified addressSource " + addressSource + " does not exist for iun " + notification.getIun() + " id " + recipient.getTaxId());
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
}
