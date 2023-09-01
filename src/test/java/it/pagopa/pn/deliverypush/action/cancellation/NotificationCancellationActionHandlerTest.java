package it.pagopa.pn.deliverypush.action.cancellation;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationCancelledDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

class NotificationCancellationActionHandlerTest {

    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    //@Mock
    //private PnAuditLogEvent logEvent;
    @Mock
    private NotificationService notificationService;

    private NotificationCancellationActionHandler handler;

    @BeforeEach
    public void setup() {

        handler = new NotificationCancellationActionHandler(
                timelineService,
                timelineUtils, notificationService);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotification() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();


        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildCancelledTimelineElement(notification)).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationService.updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, timelineElement.getTimestamp())).thenReturn(Mono.empty());

        //WHEN
        handler.cancelNotification(notification.getIun());

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(notificationService).updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, timelineElement.getTimestamp());
        //Mockito.verify(logEvent).generateSuccess();
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationAlreadyInserted() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        final TimelineElementInternal timelineElementOLD = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now().minusMillis(1000))
                .build();
        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildCancelledTimelineElement(notification)).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(timelineService.getTimelineElement(notification.getIun(), timelineElement.getElementId())).thenReturn(Optional.ofNullable(timelineElementOLD));
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationService.updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, timelineElementOLD.getTimestamp())).thenReturn(Mono.empty());

        //WHEN
        handler.cancelNotification(notification.getIun());

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(notificationService).updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, timelineElementOLD.getTimestamp());
        //Mockito.verify(logEvent).generateSuccess();
    }
}