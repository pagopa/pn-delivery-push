package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class DigitalWorkFlowUtilsTest {

    @Mock
    private TimelineService timelineService;
    @Mock
    private AddressBookService addressBookService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private NotificationUtils notificationUtils;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        addressBookService = Mockito.mock(AddressBookService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
    }

    @Test
    void getNextAddressInfo() {
    }

    @Test
    void getAddressFromSource() {
    }

    @Test
    void getScheduleDigitalWorkflowTimelineElement() {
    }

    @Test
    void getSendDigitalDetailsTimelineElement() {
    }

    @Test
    void getTimelineElement() {
    }

    @Test
    void addScheduledDigitalWorkflowToTimeline() {
    }

    @Test
    void addAvailabilitySourceToTimeline() {
    }

    @Test
    void addDigitalFeedbackTimelineElement() {
    }

    @Test
    void addDigitalDeliveringProgressTimelineElement() {
    }

    @Test
    void nextSource() {
    }
}