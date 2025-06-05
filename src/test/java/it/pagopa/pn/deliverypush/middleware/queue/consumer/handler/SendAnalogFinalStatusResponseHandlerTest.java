package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogFinalStatusResponseHandler;
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
class SendAnalogFinalStatusResponseHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private AnalogFinalStatusResponseHandler analogFinalResponseHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private SendAnalogFinalStatusResponseHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.SEND_ANALOG_FINAL_STATUS_RESPONSE, handler.getSupportedEventType());
    }

    @Test
    void handleExecutes() {
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .timelineId("timeline_123")
                .build();

        handler.handle(action, headers);

        Mockito.verify(analogFinalResponseHandler).handleFinalResponse("iun_123", 0, "timeline_123");
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Action action = Action.builder()
                .iun("iun_123")
                .recipientIndex(0)
                .timelineId("timeline_123")
                .build();

        Mockito.doThrow(new RuntimeException("Validation error")).when(analogFinalResponseHandler).handleFinalResponse("iun_123", 0, "timeline_123");

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(analogFinalResponseHandler).handleFinalResponse("iun_123", 0, "timeline_123");
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }
}