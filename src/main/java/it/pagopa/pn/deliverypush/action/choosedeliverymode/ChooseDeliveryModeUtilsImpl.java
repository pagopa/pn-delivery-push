package it.pagopa.pn.deliverypush.action.choosedeliverymode;

import it.pagopa.pn.deliverypush.action.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendCourtesyMessageDetailsInt;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

@Component
@Slf4j
public class ChooseDeliveryModeUtilsImpl implements ChooseDeliveryModeUtils{
    public static final int ZERO_SENT_ATTEMPT_NUMBER = 0;
    public static final int ONE_SENT_ATTEMPT_NUMBER = 1;

    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final CourtesyMessageUtils courtesyMessageUtils;
    private final AddressBookService addressBookService;
    private final NotificationUtils notificationUtils;

    public ChooseDeliveryModeUtilsImpl(TimelineService timelineService, TimelineUtils timelineUtils, CourtesyMessageUtils courtesyMessageUtils,
                                       AddressBookService addressBookService, NotificationUtils notificationUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.courtesyMessageUtils = courtesyMessageUtils;
        this.addressBookService = addressBookService;
        this.notificationUtils = notificationUtils;
    }

    public void addAvailabilitySourceToTimeline(Integer recIndex, NotificationInt notification, DigitalAddressSourceInt addressSource, boolean isAvailable) {
        TimelineElementInternal element = timelineUtils.buildAvailabilitySourceTimelineElement(recIndex, notification, addressSource, isAvailable, ZERO_SENT_ATTEMPT_NUMBER);
        timelineService.addTimelineElement(element, notification);
    }

    public void addScheduleAnalogWorkflowToTimeline(Integer recIndex, NotificationInt notification, Instant schedulingDate) {
        TimelineElementInternal element = timelineUtils.buildScheduleAnalogWorkflowTimeline(notification, recIndex, schedulingDate);
        timelineService.addTimelineElement(element, notification);
    }

    public Optional<SendCourtesyMessageDetailsInt> getFirstSentCourtesyMessage(String iun, Integer recIndex) {
        return courtesyMessageUtils.getSentCourtesyMessagesDetails(iun, recIndex).stream().min(Comparator.comparing(SendCourtesyMessageDetailsInt::getSendDate));
    }

    public Optional<LegalDigitalAddressInt> getPlatformAddress(NotificationInt notification, Integer recIndex) {
        NotificationRecipientInt notificationRecipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        
        return addressBookService.getPlatformAddresses(notificationRecipient.getInternalId(), notification.getSender().getPaId());
    }
    
    public LegalDigitalAddressInt getDigitalDomicile(NotificationInt notification, Integer recIndex){
        NotificationRecipientInt notificationRecipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        return notificationRecipient.getDigitalDomicile();
    }

    @Override
    public Optional<LegalDigitalAddressInt> retrievePlatformAddress(NotificationInt notification, Integer recIndex) {
        Optional<LegalDigitalAddressInt> platformAddressOpt = getPlatformAddress(notification, recIndex);
        if (platformAddressOpt.isPresent()) {
            log.info("Platform address is present, Digital workflow can be started - iun={} recipientIndex={}", notification.getIun(), recIndex);
            addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.PLATFORM, true);
        } else {
            log.info("Platform address isn't present - iun={} recipientIndex={}", notification.getIun(), recIndex);
            addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.PLATFORM, false);
        }
        return platformAddressOpt;
    }

    @Override
    public LegalDigitalAddressInt retrieveSpecialAddress(NotificationInt notification, Integer recIndex) {
        LegalDigitalAddressInt specialAddress = getDigitalDomicile(notification, recIndex);
        if (specialAddress != null && StringUtils.hasText(specialAddress.getAddress())) {
            log.info("Special address is present, Digital workflow can be started  - iun={} id={}", notification.getIun(), recIndex);
            addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.SPECIAL, true);
        } else {
            log.info("Special address isn't present, need to get General address async - iun={} recipientIndex={}", notification.getIun(), recIndex);
            addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.SPECIAL, false);
        }
        return specialAddress;
    }
}