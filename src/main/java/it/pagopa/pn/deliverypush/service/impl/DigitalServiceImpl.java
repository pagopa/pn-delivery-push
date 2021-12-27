package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.address.AttemptAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;
import it.pagopa.pn.api.dto.notification.timeline.GetAddressInfo;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.deliverypush.service.DigitalService;
import it.pagopa.pn.deliverypush.service.TimelineService;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

public class DigitalServiceImpl implements DigitalService {
    private TimelineService timelineService;

    @Override
    public AttemptAddressInfo getNextAddressInfo(String iun, String taxId) { //TODO Valutare se portarlo in classe esterna per testing
        //TODO Da rivedere i metodi utilizzati per filtrare ecc
        AttemptAddressInfo attemptAddressInfo;
        Set<TimelineElement> timeline = timelineService.getTimeline(iun);

        //Get last source tryed
        Optional<GetAddressInfo> lastAddressAttemptOpt = getLastAddressAttempt(taxId, timeline);

        if (lastAddressAttemptOpt.isPresent()) {
            GetAddressInfo lastAddressAttempt = lastAddressAttemptOpt.get();

            //Get next source to use from last used
            DigitalAddressSource2 nextAddressSource = getNextAddressSource(lastAddressAttempt.getSource());

            int attemptsMade = getAttemptsMadeForSource(taxId, timeline, nextAddressSource);

            attemptAddressInfo = AttemptAddressInfo.builder()
                    .addressSource(nextAddressSource)
                    .attemptNumberMade(attemptsMade)
                    .lastAttemptDate(lastAddressAttempt.getAttemptDate())
                    .build();
        } else {
            //TODO GESTIRE CASISTICA DI ERRORE, NON E' POSSIBILE ARRIVARE QUI
            throw new RuntimeException();
        }
        return attemptAddressInfo;
    }

    //Get last tryed source address from timeline. Attempt for source is ever added in timeline (both in case the address is available and if it's not available)
    private Optional<GetAddressInfo> getLastAddressAttempt(String taxId, Set<TimelineElement> timeline) {
        return timeline.stream()
                .filter(timelineElement -> filterLastAttemptDateInTimeline(timelineElement, taxId))
                .map(timelineElement -> (GetAddressInfo) timelineElement.getDetails()).min(Comparator.comparing(GetAddressInfo::getAttemptDate));
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
                //TODO GESTIONE ERRORE
                throw new RuntimeException();
        }
    }

}
