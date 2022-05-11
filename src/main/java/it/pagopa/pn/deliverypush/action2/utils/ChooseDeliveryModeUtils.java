package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.external.AddressBook;
import it.pagopa.pn.deliverypush.external.AddressBookEntry;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendCourtesyMessageDetails;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class ChooseDeliveryModeUtils {
    public static final int ZERO_SENT_ATTEMPT_NUMBER = 0;
    public static final int ONE_SENT_ATTEMPT_NUMBER = 1;

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final AddressBook addressBook;
    private final NotificationUtils notificationUtils;

    public ChooseDeliveryModeUtils(TimelineService timelineService, TimelineUtils timelineUtils, CourtesyMessageUtils courtesyMessageUtils, 
                                   AddressBook addressBook, NotificationUtils notificationUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.courtesyMessageUtils = courtesyMessageUtils;
        this.addressBook = addressBook;
        this.notificationUtils = notificationUtils;
    }

    public void addAvailabilitySourceToTimeline(Integer recIndex, String iun, DigitalAddressSource addressSource, boolean isAvailable) {
        TimelineElementInternal element = timelineUtils.buildAvailabilitySourceTimelineElement(recIndex, iun, addressSource, isAvailable, ZERO_SENT_ATTEMPT_NUMBER);
        timelineService.addTimelineElement(element);
    }

    public void addScheduleAnalogWorkflowToTimeline(Integer recIndex, String iun) {
        TimelineElementInternal element = timelineUtils.buildScheduleAnalogWorkflowTimeline(iun, recIndex);
        timelineService.addTimelineElement(element);
    }

    public Optional<SendCourtesyMessageDetails> getFirstSentCourtesyMessage(String iun, Integer recIndex) {
        return courtesyMessageUtils.getFirstSentCourtesyMessage(iun, recIndex);
    }

    public Optional<AddressBookEntry> getAddresses(NotificationInt notification, Integer recIndex) {
        NotificationRecipientInt notificationRecipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return addressBook.getAddresses(notificationRecipient.getTaxId(), notification.getSender());
    }
    
    public DigitalAddress getDigitalDomicile(NotificationInt notification, Integer recIndex){
        NotificationRecipientInt notificationRecipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return notificationRecipient.getDigitalDomicile();
    }
}