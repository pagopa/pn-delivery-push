package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.ext;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.PaperChannelUpdate;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PaperChannelResponseHandler;
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
class AbstractPaperChannelEventHandlerTest {

    private final PaperChannelResponseHandler paperChannelResponseHandler = Mockito.mock(PaperChannelResponseHandler.class);

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private AbstractPaperChannelEventHandler handler = new AbstractPaperChannelEventHandler(paperChannelResponseHandler) {
        @Override
        public SupportedEventType getSupportedEventType() {
            return SupportedEventType.SEND_ANALOG_RESPONSE;
        }
    };

    private static final PaperChannelUpdate PAYLOAD = new PaperChannelUpdate();

    @Test
    void getPayloadTypeReturnsCorrectType() {
        assertEquals(PaperChannelUpdate.class, handler.getPayloadType());
    }

    @Test
    void handleExecutes() {
        handler.handle(PAYLOAD, headers);

        Mockito.verify(paperChannelResponseHandler).paperChannelResponseReceiver(PAYLOAD);
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Mockito.doThrow(new RuntimeException("Validation error")).when(paperChannelResponseHandler).paperChannelResponseReceiver(PAYLOAD);

        assertThrows(RuntimeException.class, () -> handler.handle(PAYLOAD, headers));

        Mockito.verify(paperChannelResponseHandler).paperChannelResponseReceiver(PAYLOAD);
    }
}