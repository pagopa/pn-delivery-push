package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.action.cancellation.NotificationCancellationActionHandler;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimelineDaoMockTest {
    private NotificationViewedRequestHandler notificationViewedHandler;
    private NotificationService notificationService;
    private NotificationUtils notificationUtils;
    private NotificationCancellationService notificationCancellationService;
    private NotificationCancellationActionHandler notificationCancellationActionHandler;
    private PnDeliveryClientMock pnDeliveryClientMock;

    private final TimelineDaoMock timelineDaoMock = new TimelineDaoMock(notificationViewedHandler, notificationService, notificationUtils, notificationCancellationService, notificationCancellationActionHandler, pnDeliveryClientMock);


    @Test
    void getNotificationTimelineByIunWithHttpInfo() {

        timelineDaoMock.clear();
        for(int j =0; j<1000; j++) {
             ThreadPool.start(new Thread( () ->{
                 Assertions.assertDoesNotThrow( () ->
                         timelineDaoMock.addTimelineElement(TimelineElementInternal.builder().build())
                 );
             }));

            ThreadPool.start(new Thread( () ->{
                Assertions.assertDoesNotThrow( () ->
                    timelineDaoMock.getTimeline("testIun")
                );
            }));
        }
    }

}
