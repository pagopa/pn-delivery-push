package it.pagopa.pn.deliverypush.middleware.queue.consumer.router;

import it.pagopa.pn.deliverypush.exceptions.PnEventRouterException;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.EventHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.deserializer.RouterDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROUTER_EVENT_TYPE_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EventRouterTest {
    private EventRouter eventRouter;

    private EventHandlerRegistry handlerRegistry;

    private RouterDeserializer defaultRouterDeserializer;

    private EventHandler<String> mockHandler;

    private static final String DESERIALIZED_PAYLOAD = "deserializedPayload";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handlerRegistry = mock(EventHandlerRegistry.class);

        mockHandler = mock(EventHandler.class);
        when(mockHandler.getPayloadType()).thenReturn(String.class);

        defaultRouterDeserializer = mock(RouterDeserializer.class);
        when(defaultRouterDeserializer.deserialize(any(), any())).thenReturn(DESERIALIZED_PAYLOAD);

        eventRouter = new EventRouter(handlerRegistry, defaultRouterDeserializer);
    }

    @Test
    void routeThrowsExceptionWhenEventTypeExtractorReturnsNull() {
        EventRouter.RoutingConfig config = EventRouter.RoutingConfig.builder()
                .eventTypeExtractor(message -> null)
                .build();

        Message<String> message = mock(Message.class);
        when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("testHeader", "header")));

        PnEventRouterException exception = assertThrows(PnEventRouterException.class, () -> eventRouter.route(message, config));
        inspectErrorCode(ERROR_CODE_DELIVERYPUSH_ROUTER_EVENT_TYPE_MISSING, exception);
    }

    @Test
    void routeHandlesMessageOkWithDefaultConfigs() {
        Message<String> message = buildMessageWithHeadersAndPayload("CUSTOM_EVENT", "payload");

        doReturn(Optional.of(mockHandler)).when(handlerRegistry).getHandler("CUSTOM_EVENT");

        eventRouter.route(message);

        verify(mockHandler).handle("payload", message.getHeaders());
    }

    @Test
    void testForConfig_CustomEventTypeExtractor_RouteHandlesMessageOk() {
        EventRouter.RoutingConfig config = EventRouter.RoutingConfig.builder()
                .eventTypeExtractor(message -> "CUSTOM_EVENT")
                .build();

        Message<String> message = buildMessageWithHeadersAndPayload("CUSTOM_EVENT", "payload");

        doReturn(Optional.of(mockHandler)).when(handlerRegistry).getHandler("CUSTOM_EVENT");

        eventRouter.route(message, config);

        verify(mockHandler).handle("payload", message.getHeaders());
    }

    @Test
    void testForConfig_EventType_RouteHandlesMessageOk() {
        EventRouter.RoutingConfig config = EventRouter.RoutingConfig.builder()
                .eventType("CUSTOM_EVENT")
                .build();

        Message<String> message = buildMessageWithHeadersAndPayload("CUSTOM_EVENT", "payload");

        doReturn(Optional.of(mockHandler)).when(handlerRegistry).getHandler("CUSTOM_EVENT");

        eventRouter.route(message, config);

        verify(mockHandler).handle("payload", message.getHeaders());
    }

    @Test
    void testForConfig_QueueNameExtractor_RouteHandlesMessageOk() {
        EventRouter.RoutingConfig config = EventRouter.RoutingConfig.builder()
                .queueNameExtractor(m -> "customQueue")
                .build();

        Message<String> message = buildMessageWithHeadersAndPayload("CUSTOM_EVENT", "payload");

        doReturn(Optional.of(mockHandler)).when(handlerRegistry).getHandler("CUSTOM_EVENT");

        eventRouter.route(message, config);

        verify(mockHandler).handle("payload", message.getHeaders());
    }

    @Test
    void testForConfig_DeserializePayload_RouteHandlesMessageOk() {
        EventRouter.RoutingConfig config = EventRouter.RoutingConfig.builder()
                .deserializePayload(true)
                .build();

        Message<String> message = buildMessageWithHeadersAndPayload("CUSTOM_EVENT", "payload");

        doReturn(Optional.of(mockHandler)).when(handlerRegistry).getHandler("CUSTOM_EVENT");

        eventRouter.route(message, config);

        verify(mockHandler).handle(DESERIALIZED_PAYLOAD, message.getHeaders());
        verify(defaultRouterDeserializer).deserialize(message, String.class);
    }

    @Test
    void testForConfig_CustomDeserializer_RouteHandlesMessageOk() {
        String customDeserializedPayload = "customDeserializedPayload";
        RouterDeserializer customDeserializer = mock(RouterDeserializer.class);
        when(customDeserializer.deserialize(any(), any())).thenReturn(customDeserializedPayload);
        EventRouter.RoutingConfig config = EventRouter.RoutingConfig.builder()
                .customDeserializer(customDeserializer)
                .build();

        Message<String> message = buildMessageWithHeadersAndPayload("CUSTOM_EVENT", "payload");

        doReturn(Optional.of(mockHandler)).when(handlerRegistry).getHandler("CUSTOM_EVENT");

        eventRouter.route(message, config);

        verify(mockHandler).handle(customDeserializedPayload, message.getHeaders());
        verify(customDeserializer).deserialize(message, String.class);
    }

    private Message<String> buildMessageWithHeadersAndPayload(String eventType, String payload) {
        Message<String> message = mock(Message.class);
        Map<String, Object> headersMap = new HashMap<>();
        headersMap.put("eventType", eventType);
        headersMap.put("aws_messageId", "messageId123");
        headersMap.put("X-Amzn-Trace-Id", "traceId123");
        headersMap.put("iun", "iun123");
        MessageHeaders headers = new MessageHeaders(headersMap);
        when(message.getHeaders()).thenReturn(headers);
        when(message.getPayload()).thenReturn(payload);
        return message;
    }

    private void inspectErrorCode(String expectedErrorCode, PnEventRouterException exception) {
        assertEquals(expectedErrorCode, exception.getProblem().getErrors().get(0).getCode());
    }

}