package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChooseDeliveryModeUtilsTest {

    private TimelineService timelineService;
    private TimelineUtils timelineUtils;
    private CourtesyMessageUtils courtesyMessageUtils;
    private AddressBookService addressBookService;
    private NotificationUtils notificationUtils;
    private ChooseDeliveryModeUtils chooseDeliveryModeUtils;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        courtesyMessageUtils = Mockito.mock(CourtesyMessageUtils.class);
        addressBookService = Mockito.mock(AddressBookService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        chooseDeliveryModeUtils = new ChooseDeliveryModeUtils(timelineService, timelineUtils, courtesyMessageUtils, addressBookService, notificationUtils);
    }

    @Test
    void addAvailabilitySourceToTimeline() {

        Mockito.when(timelineUtils.buildAvailabilitySourceTimelineElement(Mockito.anyInt(), Mockito.any(NotificationInt.class), Mockito.any(DigitalAddressSourceInt.class), Mockito.anyBoolean(), Mockito.eq(0))).thenReturn(Mockito.any(TimelineElementInternal.class));

        chooseDeliveryModeUtils.addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class), Mockito.any(DigitalAddressSourceInt.class), Mockito.anyBoolean());

        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void addScheduleAnalogWorkflowToTimeline() {

        Mockito.when(timelineUtils.buildScheduleAnalogWorkflowTimeline(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(Mockito.any(TimelineElementInternal.class));

        chooseDeliveryModeUtils.addScheduleAnalogWorkflowToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class));

        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    void getFirstSentCourtesyMessage() {

        chooseDeliveryModeUtils.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyInt());

        Mockito.verify(timelineService, Mockito.never()).getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    void getPlatformAddress() {

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(), Mockito.eq(0))).thenReturn(Mockito.any(NotificationRecipientInt.class));

        chooseDeliveryModeUtils.getPlatformAddress(Mockito.any(), Mockito.eq(0));

        Mockito.verify(addressBookService, Mockito.never()).getPlatformAddresses(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void getDigitalDomicile() {

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(Mockito.any(NotificationRecipientInt.class));

        //Mockito.
       // chooseDeliveryModeUtils.getDigitalDomicile(Mockito.any(NotificationInt.class), Mockito.anyInt());
    }

}