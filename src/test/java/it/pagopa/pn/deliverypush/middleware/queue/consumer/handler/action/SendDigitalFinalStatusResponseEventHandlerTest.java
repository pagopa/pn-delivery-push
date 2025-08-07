package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.action;

import it.pagopa.pn.deliverypush.action.details.SendDigitalFinalStatusResponseDetails;
import it.pagopa.pn.deliverypush.action.digitalworkflow.SendDigitalFinalStatusResponseHandler;
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
class SendDigitalFinalStatusResponseEventHandlerTest {
    @Mock
    private TimelineUtils timelineUtils;

    @Mock
    private SendDigitalFinalStatusResponseHandler sendDigitalFinalStatusResponseHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private SendDigitalFinalStatusResponseEventHandler handler;

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.SEND_DIGITAL_FINAL_STATUS_RESPONSE, handler.getSupportedEventType());
    }

    @Test
    void handleExecutes() {
        SendDigitalFinalStatusResponseDetails details = new SendDigitalFinalStatusResponseDetails();
        Action action = Action.builder()
                .iun("iun_123")
                .details(details)
                .build();

        handler.handle(action, headers);

        Mockito.verify(sendDigitalFinalStatusResponseHandler).handleSendDigitalFinalStatusResponse("iun_123", details);
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        SendDigitalFinalStatusResponseDetails details = new SendDigitalFinalStatusResponseDetails();
        Action action = Action.builder()
                .iun("iun_123")
                .details(details)
                .build();

        Mockito.doThrow(new RuntimeException("Validation error")).when(sendDigitalFinalStatusResponseHandler).handleSendDigitalFinalStatusResponse("iun_123", details);

        assertThrows(RuntimeException.class, () -> handler.handle(action, headers));

        Mockito.verify(sendDigitalFinalStatusResponseHandler).handleSendDigitalFinalStatusResponse("iun_123", details);
        Mockito.verify(timelineUtils, Mockito.never()).checkIsNotificationCancellationRequested("iun_123");
    }
}