package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.AttemptAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;
import it.pagopa.pn.api.dto.notification.timeline.GetAddressInfo;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.DigitaWorkFlowService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class DigitalWorkFlowServiceImpl implements DigitaWorkFlowService {
    private final TimelineService timelineService;
    private final PublicRegistryService publicRegistryService;
    private final AddressBookService addressBookService;

    public DigitalWorkFlowServiceImpl(TimelineService timelineService, PublicRegistryService publicRegistryService, AddressBookService addressBookService) {
        this.timelineService = timelineService;
        this.publicRegistryService = publicRegistryService;
        this.addressBookService = addressBookService;
    }

    @Override
    public AttemptAddressInfo getNextAddressInfo(String iun, String taxId) {
        log.debug("Start getNextAddressInfo for iun {} id {}", iun, taxId);

        //TODO Da rivedere i metodi utilizzati per filtrare ecc
        AttemptAddressInfo attemptAddressInfo;
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);

        //Viene ottenuto l'ultimo indirizzo utilizzato
        GetAddressInfo lastAddressAttempt = getLastAddressAttempt(taxId, timeline);
        log.debug("Get last address attempt with source {}", lastAddressAttempt.getSource());

        //Ottiene la source del prossimo indirizzo da utilizzare
        DigitalAddressSource2 nextAddressSource = getNextAddressSource(lastAddressAttempt.getSource());
        log.debug("nextAddressSource {}", nextAddressSource);

        //Ottiene i tentativi effettuati per tale indirizzo
        int attemptsMade = getAttemptsMadeForSource(taxId, timeline, nextAddressSource);
        log.debug("AttemptsMade for source {} is {}", nextAddressSource, attemptsMade);

        attemptAddressInfo = AttemptAddressInfo.builder()
                .addressSource(nextAddressSource)
                .sentAttemptMade(attemptsMade)
                .lastAttemptDate(lastAddressAttempt.getAttemptDate())
                .build();

        return attemptAddressInfo;
    }

    //Get last tryed source address from timeline. Attempt for source is ever added in timeline (both in case the address is available and if it's not available)
    private GetAddressInfo getLastAddressAttempt(String taxId, Set<TimelineElement> timeline) {

        Optional<GetAddressInfo> lastAddressAttemptOpt = timeline.stream()
                .filter(timelineElement -> filterLastAttemptDateInTimeline(timelineElement, taxId))
                .map(timelineElement -> (GetAddressInfo) timelineElement.getDetails()).min(Comparator.comparing(GetAddressInfo::getAttemptDate));

        if (lastAddressAttemptOpt.isPresent()) {
            return lastAddressAttemptOpt.get();
        } else {
            log.error("Last address attempt not found for taxId {}", taxId);
            throw new PnInternalException("Last address attempt not found for taxId " + taxId);
        }
    }

    private boolean filterLastAttemptDateInTimeline(TimelineElement el, String taxId) {
        boolean availableAdressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAdressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId());
        }
        return false;
    }

    // Get attempts number made for passed source
    private int getAttemptsMadeForSource(String taxId, Set<TimelineElement> timeline, DigitalAddressSource2 nextAddressSource) {
        return (int) timeline.stream()
                .filter(timelineElement -> filterTimelineForTaxIdAndSource(timelineElement, taxId, nextAddressSource)).count();
    }

    private boolean filterTimelineForTaxIdAndSource(TimelineElement el, String taxId, DigitalAddressSource2 source) {
        boolean availableAddressCategory = TimelineElementCategory.GET_ADDRESS.equals(el.getCategory());
        if (availableAddressCategory) {
            GetAddressInfo details = (GetAddressInfo) el.getDetails();
            return taxId.equalsIgnoreCase(details.getTaxId()) && source.equals(details.getSource());
        }
        return false;
    }

    /**
     * Get next address source from passed source in this order: PLATFORM, GENERAL, SPECIAL
     *
     * @param addressSource
     * @return next address source
     */
    public DigitalAddressSource2 getNextAddressSource(DigitalAddressSource2 addressSource) {
        switch (addressSource) {
            case PLATFORM:
                return DigitalAddressSource2.GENERAL;
            case GENERAL:
                return DigitalAddressSource2.SPECIAL;
            case SPECIAL:
                return DigitalAddressSource2.PLATFORM;
            default:
                log.error("Address source {} is not valid", addressSource);
                throw new PnInternalException("Address source " + addressSource + " is not valid");
        }
    }

    @Nullable
    @Override
    public DigitalAddress getAddressFromSource(DigitalAddressSource2 addressSource, NotificationRecipient recipient, Notification notification) {
        DigitalAddress destinationAddress;
        switch (addressSource) {
            case PLATFORM:
                destinationAddress = addressBookService.retrievePlatformAddress(recipient, notification.getSender());
                break;
            case SPECIAL:
                destinationAddress = recipient.getDigitalDomicile();
                break;
            default:
                log.error("Specified addressSource {} does not exist for iun {} id {}", addressSource, notification.getIun(), recipient.getTaxId());
                throw new PnInternalException("Specified addressSource " + addressSource + " does not exist for iun " + notification.getIun() + " id " + recipient.getTaxId());
        }
        return destinationAddress;
    }

}
