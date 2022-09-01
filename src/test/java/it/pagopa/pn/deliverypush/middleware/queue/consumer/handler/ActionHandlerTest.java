package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.*;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl.WebhookActionsEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;

class ActionHandlerTest {

    @Mock
    private DigitalWorkFlowHandler digitalWorkFlowHandler;

    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;

    @Mock
    private RefinementHandler refinementHandler;

    @Mock
    private WebhookActionsEventHandler webhookActionsEventHandler;

    @Mock
    private StartWorkflowForRecipientHandler startWorkflowForRecipientHandler;

    @Mock
    private ChooseDeliveryModeHandler chooseDeliveryModeHandler;

    private ActionHandler handler;

    @BeforeEach
    void setup() {
        digitalWorkFlowHandler = Mockito.mock(DigitalWorkFlowHandler.class);
        analogWorkflowHandler = Mockito.mock(AnalogWorkflowHandler.class);
        refinementHandler = Mockito.mock(RefinementHandler.class);
        webhookActionsEventHandler = Mockito.mock(WebhookActionsEventHandler.class);
        startWorkflowForRecipientHandler = Mockito.mock(StartWorkflowForRecipientHandler.class);
        chooseDeliveryModeHandler = Mockito.mock(ChooseDeliveryModeHandler.class);
        handler = new ActionHandler(digitalWorkFlowHandler, analogWorkflowHandler, refinementHandler, webhookActionsEventHandler, startWorkflowForRecipientHandler, chooseDeliveryModeHandler);
    }

    @Test
    void pnDeliveryPushStartRecipientWorkflow() {
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
       
        /***
         * 
         *   Gateway gateway = Mockito.mock(Gateway.class);
         *
         *     doAnswer(ans -> {
         *         Consumer<UUID> callback = (Consumer<UUID>) ans.getArgument(1);
         *         callback.accept(uuid);
         *         return null;
         *     }).when(gateway).process(Mockito.any(String.class), Mockito.any(Consumer.class));
         *
         *     Interactor.builder().gateway(gateway).build().process("ignored", s -> {
         *         Assertions.assertEquals(uuid.toString(), s);
         *     });
         */
        
    }

    @Test
    void pnDeliveryPushChooseDeliveryMode() {
    }

    @Test
    void pnDeliveryPushAnalogWorkflowConsumer() {
    }

    @Test
    void pnDeliveryPushRefinementConsumer() {
    }

    @Test
    void pnDeliveryPushDigitalNextActionConsumer() {
    }

    @Test
    void pnDeliveryPushWebhookActionConsumer() {
    }

    private Action buildAction(ActionType type) {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        return Action.builder()
                .iun("01")
                .actionId("02")
                .notBefore(instant)
                .type(type)
                .recipientIndex(3)
                .build();
    }
}