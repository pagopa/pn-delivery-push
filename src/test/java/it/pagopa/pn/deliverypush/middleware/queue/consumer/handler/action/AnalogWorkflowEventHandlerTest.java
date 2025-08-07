package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
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
class AnalogWorkflowEventHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private AnalogWorkflowEventHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.ANALOG_WORKFLOW, handler.getSupportedEventType());
    }

    @Test
    void handleExecutesWhenNotificationNotCancelled() {
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);

        handler.handle(action, headers);

        Mockito.verify(analogWorkflowHandler).startAnalogWorkflow("iun_123", 0);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleDoesNotExecuteWhenNotificationCancelled() {
        RecipientsWorkflowDetails details = new RecipientsWorkflowDetails();
        Action action = Action.builder()
                .iun("iun_123")
                .details(details)
                .recipientIndex(0)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(true);

        handler.handle(action, headers);

        Mockito.verify(analogWorkflowHandler, Mockito.never()).startAnalogWorkflow("iun_123", 0);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        RecipientsWorkflowDetails details = new RecipientsWorkflowDetails();
        Action action = Action.builder()
                .iun("iun_123")
                .details(details)
                .recipientIndex(0)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);
        Mockito.doThrow(new RuntimeException("Validation error")).when(analogWorkflowHandler).startAnalogWorkflow("iun_123", 0);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(analogWorkflowHandler).startAnalogWorkflow("iun_123", 0);

        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }
}