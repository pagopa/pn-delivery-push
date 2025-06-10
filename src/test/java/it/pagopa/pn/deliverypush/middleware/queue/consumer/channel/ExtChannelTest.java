package it.pagopa.pn.deliverypush.middleware.queue.consumer.channel;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.EventRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtChannelTest {

    private EventRouter eventRouter;
    private ExtChannel handler;

    @BeforeEach
    void setUp() {
        eventRouter = mock(EventRouter.class);
        handler = new ExtChannel(eventRouter);
    }

    @Test
    void routesMessageWithEventTypeHeader() {
        SingleStatusUpdate payload = new SingleStatusUpdate();
        Message<SingleStatusUpdate> message = MessageBuilder.withPayload(payload)
                .setHeader("eventType", "CUSTOM_EVENT")
                .build();

        handler.pnExtChannelEventInboundConsumer().accept(message);

        ArgumentCaptor<EventRouter.RoutingConfig> configCaptor = ArgumentCaptor.forClass(EventRouter.RoutingConfig.class);
        verify(eventRouter).route(eq(message), configCaptor.capture());
        assertEquals("CUSTOM_EVENT", configCaptor.getValue().getEventType());
        assertTrue(configCaptor.getValue().isDeserializePayload());
    }

    @Test
    void routesMessageWithDefaultEventTypeWhenHeaderMissing() {
        SingleStatusUpdate payload = new SingleStatusUpdate();
        Message<SingleStatusUpdate> message = MessageBuilder.withPayload(payload).build();

        handler.pnExtChannelEventInboundConsumer().accept(message);

        ArgumentCaptor<EventRouter.RoutingConfig> configCaptor = ArgumentCaptor.forClass(EventRouter.RoutingConfig.class);
        verify(eventRouter).route(eq(message), configCaptor.capture());
        assertEquals("SEND_PEC_RESPONSE", configCaptor.getValue().getEventType());
    }

    @Test
    void routesMessageWithDefaultEventTypeWhenHeaderEmpty() {
        SingleStatusUpdate payload = new SingleStatusUpdate();
        Message<SingleStatusUpdate> message = MessageBuilder.withPayload(payload)
                .setHeader("eventType", "")
                .build();

        handler.pnExtChannelEventInboundConsumer().accept(message);

        ArgumentCaptor<EventRouter.RoutingConfig> configCaptor = ArgumentCaptor.forClass(EventRouter.RoutingConfig.class);
        verify(eventRouter).route(eq(message), configCaptor.capture());
        assertEquals("SEND_PEC_RESPONSE", configCaptor.getValue().getEventType());
    }

    @Test
    void handlesExceptionAndRethrows() {
        SingleStatusUpdate payload = new SingleStatusUpdate();
        Message<SingleStatusUpdate> message = MessageBuilder.withPayload(payload)
                .setHeader("eventType", "CUSTOM_EVENT")
                .build();

        doThrow(new RuntimeException("fail")).when(eventRouter).route(any(), any());

        assertThrows(RuntimeException.class, () -> handler.pnExtChannelEventInboundConsumer().accept(message));
    }
}