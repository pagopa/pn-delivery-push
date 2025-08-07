package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.checkattachmentretention.CheckAttachmentRetentionHandler;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CheckAttachmentRetentionEventHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private CheckAttachmentRetentionEventHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.CHECK_ATTACHMENT_RETENTION, handler.getSupportedEventType());
    }

    @Test
    void handleExecutes() {
        Instant notBefore = Instant.now();
        Action action = Action.builder()
                .iun("iun_123")
                .notBefore(notBefore)
                .build();

        handler.handle(action, headers);

        Mockito.verify(checkAttachmentRetentionHandler).handleCheckAttachmentRetentionBeforeExpiration("iun_123", notBefore);
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Instant notBefore = Instant.now();
        Action action = Action.builder()
                .iun("iun_123")
                .notBefore(notBefore)
                .build();

        Mockito.doThrow(new RuntimeException("Validation error")).when(checkAttachmentRetentionHandler).handleCheckAttachmentRetentionBeforeExpiration("iun_123", notBefore);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(checkAttachmentRetentionHandler).handleCheckAttachmentRetentionBeforeExpiration("iun_123", notBefore);
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }
}