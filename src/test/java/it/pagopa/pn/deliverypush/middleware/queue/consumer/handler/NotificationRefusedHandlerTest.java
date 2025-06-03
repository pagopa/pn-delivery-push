package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.details.NotificationRefusedActionDetails;
import it.pagopa.pn.deliverypush.action.refused.NotificationRefusedActionHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class NotificationRefusedHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private NotificationRefusedActionHandler notificationRefusedActionHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private NotificationRefusedHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.NOTIFICATION_REFUSED, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesRefusal() {
        NotificationRefusedActionDetails details = NotificationRefusedActionDetails.builder()
                .errors(List.of(NotificationRefusedErrorInt.builder()
                        .errorCode("error_code")
                        .detail("error_detail")
                        .build()))
                .build();
        Instant notBefore = Instant.now();
        Action action = Action.builder()
                .iun("iun_123")
                .details(details)
                .notBefore(notBefore)
                .build();

        handler.handle(action, headers);

        Mockito.verify(notificationRefusedActionHandler).notificationRefusedHandler("iun_123", details.getErrors(), notBefore);
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        NotificationRefusedActionDetails details = NotificationRefusedActionDetails.builder()
                .errors(List.of(NotificationRefusedErrorInt.builder()
                        .errorCode("error_code")
                        .detail("error_detail")
                        .build()))
                .build();
        Instant notBefore = Instant.now();
        Action action = Action.builder()
                .iun("iun_123")
                .details(details)
                .notBefore(notBefore)
                .build();

        Mockito.doThrow(new RuntimeException("Validation error")).when(notificationRefusedActionHandler).notificationRefusedHandler("iun_123", details.getErrors(), notBefore);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(notificationRefusedActionHandler).notificationRefusedHandler("iun_123", details.getErrors(), notBefore);
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }

}