package it.pagopa.pn.deliverypush.middleware.queue.consumer.channel;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.EventRouter;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
class ActionChannelTest {
    @InjectMocks
    private ActionChannel actionChannel;
    @Mock
    private EventRouter eventRouter;

    @Test
    void pnDeliveryPushValidationActionsInboundConsumer_routesMessageSuccessfully() {
        Action action = Action.builder().iun("test_IUN").recipientIndex(0).type(ActionType.NOTIFICATION_VALIDATION).build();
        Message<Action> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(action);
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("test", "headerValue")));

        actionChannel.pnDeliveryPushValidationActionsInboundConsumer().accept(message);

        EventRouter.RoutingConfig expectedConfig = EventRouter.RoutingConfig.builder()
                .eventType(ActionType.NOTIFICATION_VALIDATION.name())
                .build();
        Mockito.verify(eventRouter).route(message, expectedConfig);
    }

    @Test
    void pnDeliveryPushValidationActionsInboundConsumer_handlesExceptionGracefully() {
        Action action = Action.builder().iun("test_IUN").recipientIndex(0).build();
        Message<Action> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(action);
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("test", "headerValue")));

        Consumer<Message<Action>> consumer = actionChannel.pnDeliveryPushValidationActionsInboundConsumer();
        assertThrows(RuntimeException.class, () ->
                consumer.accept(message)
        );

        Mockito.verify(eventRouter, Mockito.never()).route(Mockito.any(), Mockito.any());
    }

    @Test
    void pnDeliveryPushActionsInboundConsumer_routesMessageSuccessfully() {
        Action action = Action.builder().iun("test_IUN").recipientIndex(0).type(ActionType.NOTIFICATION_VALIDATION).build();
        Message<Action> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(action);
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("test", "headerValue")));

        actionChannel.pnDeliveryPushActionsInboundConsumer().accept(message);

        EventRouter.RoutingConfig expectedConfig = EventRouter.RoutingConfig.builder()
                .eventType(ActionType.NOTIFICATION_VALIDATION.name())
                .build();
        Mockito.verify(eventRouter).route(message, expectedConfig);
    }

    @Test
    void pnDeliveryPushActionsInboundConsumer_handlesExceptionGracefully() {
        Action action = Action.builder().iun("test_IUN").recipientIndex(0).build();
        Message<Action> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(action);
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("test", "headerValue")));

        Consumer<Message<Action>> consumer = actionChannel.pnDeliveryPushActionsInboundConsumer();
        assertThrows(RuntimeException.class, () ->consumer.accept(message));

        Mockito.verify(eventRouter, Mockito.never()).route(Mockito.any(), Mockito.any());
    }


    @NotNull
    private static Message<Action> getActionMessage() {
        return new Message<>() {
            @Override
            @NotNull
            public Action getPayload() {
                return Action.builder()
                        .iun("test_IUN")
                        .recipientIndex(0)
                        .timelineId("testTimelineId")
                        .notBefore(Instant.EPOCH)
                        .build();
            }

            @Override
            @NotNull
            public MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
    }

}