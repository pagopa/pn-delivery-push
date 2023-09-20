package it.pagopa.pn.deliverypush.action.cancellation;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationCancelledDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

class NotificationCancellationActionHandlerTest {

    @Mock
    private NotificationCancellationService notificationCancellationService;

    private NotificationCancellationActionHandler handler;

    @BeforeEach
    public void setup() {

        handler = new NotificationCancellationActionHandler(
                notificationCancellationService);
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
        Mockito.doNothing().when(notificationCancellationService).completeCancellationProcess(notification.getIun());

        //WHEN
        handler.cancelNotification(notification.getIun());

        //THEN
        Mockito.verify(notificationCancellationService).completeCancellationProcess(notification.getIun());
    }

}