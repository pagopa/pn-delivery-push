package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.ext;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
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
class SendPecResponseEventHandlerTest {
    @Mock
    private ExternalChannelResponseHandler externalChannelResponseHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private SendPecResponseEventHandler handler;

    private static final SingleStatusUpdate PAYLOAD = new SingleStatusUpdate();

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.SEND_PEC_RESPONSE, handler.getSupportedEventType());
    }

    @Test
    void getPayloadTypeReturnsCorrectType() {
        assertEquals(SingleStatusUpdate.class, handler.getPayloadType());
    }

    @Test
    void handleExecutes() {
        handler.handle(PAYLOAD, headers);

        Mockito.verify(externalChannelResponseHandler).extChannelResponseReceiver(PAYLOAD);
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Mockito.doThrow(new RuntimeException("Validation error")).when(externalChannelResponseHandler).extChannelResponseReceiver(PAYLOAD);

        assertThrows(RuntimeException.class, () -> handler.handle(PAYLOAD, headers));

        Mockito.verify(externalChannelResponseHandler).extChannelResponseReceiver(PAYLOAD);
    }

}