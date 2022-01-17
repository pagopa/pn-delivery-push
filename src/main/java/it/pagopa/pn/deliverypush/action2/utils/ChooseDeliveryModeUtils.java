package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyMessageDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ChooseDeliveryModeUtils {
    public static final int ZERO_SENT_ATTEMPT_NUMBER = 0;
    public static final int ONE_SENT_ATTEMPT_NUMBER = 0;

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final AddressBook addressBook;

    public ChooseDeliveryModeUtils(TimelineService timelineService, TimelineUtils timelineUtils, CourtesyMessageUtils courtesyMessageUtils, AddressBook addressBook) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.courtesyMessageUtils = courtesyMessageUtils;
        this.addressBook = addressBook;
    }

    public void addAvailabilitySourceToTimeline(String taxId, String iun, DigitalAddressSource addressSource, boolean isAvailable) {
        TimelineElement element = timelineUtils.buildAvailabilitySourceTimelineElement(taxId, iun, addressSource, isAvailable, ZERO_SENT_ATTEMPT_NUMBER);
        timelineService.addTimelineElement(element);
    }

    public void addScheduleAnalogWorkflowToTimeline(String taxId, String iun) {
        TimelineElement element = timelineUtils.buildScheduleAnalogWorkflowTimeline(iun, taxId);
        timelineService.addTimelineElement(element);
    }

    public Optional<SendCourtesyMessageDetails> getFirstSentCourtesyMessage(String iun, String taxId) {
        return courtesyMessageUtils.getFirstSentCourtesyMessage(iun, taxId);
    }

    public Optional<AddressBookEntry> getAddresses(String taxId, NotificationSender sender) {
        return addressBook.getAddresses(taxId, sender);
    }
}