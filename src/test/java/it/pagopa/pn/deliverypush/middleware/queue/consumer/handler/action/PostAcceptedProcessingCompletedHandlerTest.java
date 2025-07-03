package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.startworkflow.ScheduleRecipientWorkflow;
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
class PostAcceptedProcessingCompletedHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private ScheduleRecipientWorkflow scheduleRecipientWorkflow;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private PostAcceptedProcessingCompletedHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.POST_ACCEPTED_PROCESSING_COMPLETED, handler.getSupportedEventType());
    }

    @Test
    void handleExecutes() {
        Action action = Action.builder()
                .iun("iun_123")
                .build();

        handler.handle(action, headers);

        Mockito.verify(scheduleRecipientWorkflow).startScheduleRecipientWorkflow("iun_123");
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Action action = Action.builder()
                .iun("iun_123")
                .build();

        Mockito.doThrow(new RuntimeException("Validation error")).when(scheduleRecipientWorkflow).startScheduleRecipientWorkflow("iun_123");

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(scheduleRecipientWorkflow).startScheduleRecipientWorkflow("iun_123");
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }
}