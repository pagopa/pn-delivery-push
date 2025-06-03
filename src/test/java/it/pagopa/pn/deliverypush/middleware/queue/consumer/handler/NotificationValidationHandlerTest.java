package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationValidationHandlerTest {

    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private NotificationValidationActionHandler notificationValidationActionHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private NotificationValidationHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.NOTIFICATION_VALIDATION, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesValidationWhenNotificationNotCancelled() {
        NotificationValidationActionDetails details = new NotificationValidationActionDetails();
        Action action = Action.builder()
                .iun("iun_123")
                .details(details)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);

        handler.handle(action, headers);

        Mockito.verify(notificationValidationActionHandler).validateNotification("iun_123", details);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleDoesNotExecuteValidationWhenNotificationCancelled() {
        Action action = Action.builder().iun("iun_123").build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(true);

        handler.handle(action, headers);

        Mockito.verify(notificationValidationActionHandler, Mockito.never()).validateNotification(Mockito.anyString(), Mockito.any());
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        NotificationValidationActionDetails details = new NotificationValidationActionDetails();
        Action action = Action.builder()
                .iun("iun_123")
                .details(details)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);
        Mockito.doThrow(new RuntimeException("Validation error")).when(notificationValidationActionHandler).validateNotification("iun_123", details);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(notificationValidationActionHandler).validateNotification("iun_123", details);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }
}