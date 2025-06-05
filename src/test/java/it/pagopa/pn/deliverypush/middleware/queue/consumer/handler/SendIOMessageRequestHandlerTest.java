package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnExtRegistryIOSentMessageEvent;
import it.pagopa.pn.deliverypush.action.iosentmessage.IOSentMessageHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
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
class SendIOMessageRequestHandlerTest {
    @Mock
    private IOSentMessageHandler ioSentMessageHandler;

    @Mock
    private MessageHeaders headers;

    @InjectMocks
    private SendIOMessageRequestHandler handler;

    private static final String IUN = "iun_123";
    private static final int REC_INDEX = 1;
    private static final Instant SEND_DATE = Instant.now();

    private static final PnExtRegistryIOSentMessageEvent.Payload PAYLOAD = PnExtRegistryIOSentMessageEvent.Payload.builder()
            .iun(IUN)
            .recIndex(REC_INDEX)
            .sendDate(SEND_DATE)
            .build();

    @Test
    void getSupportedEventTypeReturnsCorrectType() {
        assertEquals(SupportedEventType.SEND_IO_MESSAGE_REQUEST, handler.getSupportedEventType());
    }

    @Test
    void getPayloadTypeReturnsCorrectType() {
        assertEquals(PnExtRegistryIOSentMessageEvent.Payload.class, handler.getPayloadType());
    }

    @Test
    void handleExecutes() {
        handler.handle(PAYLOAD, headers);

        Mockito.verify(ioSentMessageHandler).handleIOSentMessage(IUN, REC_INDEX, SEND_DATE);
    }

    @Test
    void handleLogsAndThrowsExceptionOnError() {
        Mockito.doThrow(new RuntimeException("Validation error")).when(ioSentMessageHandler).handleIOSentMessage(IUN, REC_INDEX, SEND_DATE);

        assertThrows(RuntimeException.class, () -> handler.handle(PAYLOAD, headers));

        Mockito.verify(ioSentMessageHandler).handleIOSentMessage(IUN, REC_INDEX, SEND_DATE);
    }
}