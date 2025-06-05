package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowRetryHandler;
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
class DigitalWorkflowNoResponseTimeoutActionHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private DigitalWorkflowNoResponseTimeoutActionHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesWhenNotificationNotCancelled() {
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .timelineId("timeline_123")
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);

        handler.handle(action, headers);

        Mockito.verify(digitalWorkFlowRetryHandler).elapsedExtChannelTimeout("iun_123", 0, "timeline_123");
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleDoesNotExecuteWhenNotificationCancelled() {
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .timelineId("timeline_123")
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(true);

        handler.handle(action, headers);

        Mockito.verify(digitalWorkFlowRetryHandler, Mockito.never()).elapsedExtChannelTimeout("iun_123", 0, "timeline_123");
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .timelineId("timeline_123")
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);
        Mockito.doThrow(new RuntimeException("Validation error")).when(digitalWorkFlowRetryHandler).elapsedExtChannelTimeout("iun_123", 0, "timeline_123");

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(digitalWorkFlowRetryHandler).elapsedExtChannelTimeout("iun_123", 0, "timeline_123");

        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }
}