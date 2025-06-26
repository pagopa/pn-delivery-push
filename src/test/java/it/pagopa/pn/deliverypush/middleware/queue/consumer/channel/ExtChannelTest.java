package it.pagopa.pn.deliverypush.middleware.queue.consumer.channel;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.EventRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtChannelTest {

    private EventRouter eventRouter;
    private ExtChannel handler;
    private static final String PAYLOAD = "payload";

    @BeforeEach
    void setUp() {
        eventRouter = mock(EventRouter.class);
        handler = new ExtChannel(eventRouter);
    }

    @Test
    void routesMessageWithEventTypeHeader() {
        Message<String> message = MessageBuilder.withPayload(PAYLOAD)
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
        Message<String> message = MessageBuilder.withPayload(PAYLOAD).build();

        handler.pnExtChannelEventInboundConsumer().accept(message);

        ArgumentCaptor<EventRouter.RoutingConfig> configCaptor = ArgumentCaptor.forClass(EventRouter.RoutingConfig.class);
        verify(eventRouter).route(eq(message), configCaptor.capture());
        assertEquals("SEND_PEC_RESPONSE", configCaptor.getValue().getEventType());
    }

    @Test
    void routesMessageWithEventTypeMockExtChannel() {
        Message<String> message = MessageBuilder.withPayload(PAYLOAD)
                .setHeader("eventType", "EXTERNAL_CHANNELS_EVENT")
                .build();

        handler.pnExtChannelEventInboundConsumer().accept(message);

        ArgumentCaptor<EventRouter.RoutingConfig> configCaptor = ArgumentCaptor.forClass(EventRouter.RoutingConfig.class);
        verify(eventRouter).route(eq(message), configCaptor.capture());
        assertEquals("SEND_PEC_RESPONSE", configCaptor.getValue().getEventType());
    }

    @Test
    void routesMessageWithDefaultEventTypeWhenHeaderEmpty() {
        Message<String> message = MessageBuilder.withPayload(PAYLOAD)
                .setHeader("eventType", "")
                .build();

        handler.pnExtChannelEventInboundConsumer().accept(message);

        ArgumentCaptor<EventRouter.RoutingConfig> configCaptor = ArgumentCaptor.forClass(EventRouter.RoutingConfig.class);
        verify(eventRouter).route(eq(message), configCaptor.capture());
        assertEquals("SEND_PEC_RESPONSE", configCaptor.getValue().getEventType());
    }

    @Test
    void handlesExceptionAndRethrows() {
        Message<String> message = MessageBuilder.withPayload(PAYLOAD)
                .setHeader("eventType", "CUSTOM_EVENT")
                .build();

        doThrow(new RuntimeException("fail")).when(eventRouter).route(any(), any());

        Consumer<Message<String>> consumer = handler.pnExtChannelEventInboundConsumer();
        assertThrows(RuntimeException.class, () -> consumer.accept(message));
    }
}