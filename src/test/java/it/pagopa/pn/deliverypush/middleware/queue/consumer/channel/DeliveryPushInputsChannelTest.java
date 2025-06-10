package it.pagopa.pn.deliverypush.middleware.queue.consumer.channel;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.EventRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
class DeliveryPushInputsChannelTest {
    @InjectMocks
    private DeliveryPushInputsChannel handler;

    @Mock
    private EventRouter eventRouter;

    @Test
    void pnDeliveryPushInputsInboundConsumer_routesMessageSuccessfully() {
        Message<String> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn("Test Payload");
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("eventType", "NOTIFICATION_VALIDATION")));

        handler.pnDeliveryPushInputsInboundConsumer().accept(message);

        EventRouter.RoutingConfig expectedConfig = EventRouter.RoutingConfig.builder()
                .deserializePayload(true)
                .build();
        Mockito.verify(eventRouter).route(message, expectedConfig);
    }

    @Test
    void pnDeliveryPushInputsInboundConsumer_handlesExceptionGracefully() {
        Message<String> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn("Test Payload");
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("eventType", "NOTIFICATION_VALIDATION")));

        Mockito.doThrow(new RuntimeException("Test Exception"))
                .when(eventRouter).route(Mockito.any(), Mockito.any());

        assertThrows(RuntimeException.class, () ->
                handler.pnDeliveryPushInputsInboundConsumer().accept(message)
        );

        EventRouter.RoutingConfig expectedConfig = EventRouter.RoutingConfig.builder()
                .deserializePayload(true)
                .build();
        Mockito.verify(eventRouter).route(message, expectedConfig);
    }

}