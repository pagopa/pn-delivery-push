package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.action.NotificationViewedHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimelineDaoMockTest {
    private NotificationViewedHandler notificationViewedHandler;
    private NotificationService notificationService;
    private NotificationUtils notificationUtils;

    private final TimelineDaoMock timelineDaoMock = new TimelineDaoMock(notificationViewedHandler, notificationService, notificationUtils);


    @Test
    void getNotificationTimelineByIunWithHttpInfo() {

        timelineDaoMock.clear();
        for(int j =0; j<1000; j++) {
            new Thread( () ->{
                Assertions.assertDoesNotThrow( () ->
                        timelineDaoMock.addTimelineElement(TimelineElementInternal.builder().build())
                );
            }
            ).start();

            new Thread( () ->{
                Assertions.assertDoesNotThrow( () ->
                    timelineDaoMock.getTimeline("testIun")
                );
            }).start();
        }
    }

}