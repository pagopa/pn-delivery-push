package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

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

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AbstractActionEventHandlerTest {

    private final TimelineUtils timelineUtils = Mockito.mock(TimelineUtils.class);

    @Mock
    private Consumer<Action> functionToCall;

    @InjectMocks
    private AbstractActionEventHandler handler = new AbstractActionEventHandler(timelineUtils) {
        @Override
        public SupportedEventType getSupportedEventType() {
            return SupportedEventType.START_RECIPIENT_WORKFLOW;
        }

        @Override
        public void handle(Action payload, MessageHeaders headers) {
            // No-op for testing
        }
    };

    @Test
    void checkNotificationCancelledAndExecute_executesFunctionWhenNotificationNotCancelled() {
        Action action = Action.builder().iun("iun_123").build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(false);

        handler.checkNotificationCancelledAndExecute(action, functionToCall);

        Mockito.verify(functionToCall).accept(action);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void checkNotificationCancelledAndExecute_doesNotExecuteFunctionWhenNotificationCancelled() {
        Action action = Action.builder().iun("iun_123").build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested("iun_123")).thenReturn(true);

        handler.checkNotificationCancelledAndExecute(action, functionToCall);

        Mockito.verify(functionToCall, Mockito.never()).accept(action);
        Mockito.verify(timelineUtils).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void getPayloadType_returnsActionClass() {
        assertEquals(Action.class, handler.getPayloadType());
    }

    @Test
    void getSupportedEventType_returnsCorrectEventType() {
        assertEquals(SupportedEventType.START_RECIPIENT_WORKFLOW, handler.getSupportedEventType());
    }
}