package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RefinementNotificationHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private RefinementHandler refinementHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private RefinementNotificationHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.REFINEMENT_NOTIFICATION, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesWhenNotificationNotCancelled() {
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);

        handler.handle(action, headers);

        Mockito.verify(refinementHandler).handleRefinement("iun_123", 0);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleDoesNotExecuteWhenNotificationCancelled() {
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(true);

        handler.handle(action, headers);

        Mockito.verify(refinementHandler, Mockito.never()).handleRefinement("iun_123", 0);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);
        Mockito.doThrow(new RuntimeException("Validation error")).when(refinementHandler).handleRefinement("iun_123", 0);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(refinementHandler).handleRefinement("iun_123", 0);

        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }
}