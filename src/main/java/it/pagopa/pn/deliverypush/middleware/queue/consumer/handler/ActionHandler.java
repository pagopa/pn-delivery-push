package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.impl.WebhookActionsEventHandler;
import it.pagopa.pn.deliverypush.action.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.RefinementHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class ActionHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final RefinementHandler refinementHandler;
    private final WebhookActionsEventHandler webhookActionsEventHandler;

    public ActionHandler(DigitalWorkFlowHandler digitalWorkFlowHandler,
                         AnalogWorkflowHandler analogWorkflowHandler,
                         RefinementHandler refinementHandler,
                         WebhookActionsEventHandler webhookActionsEventHandler) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.refinementHandler = refinementHandler;
        this.webhookActionsEventHandler = webhookActionsEventHandler;
    }
    
    @Bean
    public Consumer<Message<Action>> pnDeliveryPushAnalogWorkflowConsumer() {
        return message -> {
            log.info("pnDeliveryPushAnalogWorkflowConsumer, message {}", message);
            Action action = message.getPayload();
            analogWorkflowHandler.startAnalogWorkflow(action.getIun(), action.getRecipientIndex());
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushRefinementConsumer() {
        return message -> {
            log.info("pnDeliveryPushRefinementConsumer, message {}", message);
            Action action = message.getPayload();
            refinementHandler.handleRefinement(action.getIun(), action.getRecipientIndex());
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalNextActionConsumer() {
        return message -> {
            log.info("pnDeliveryPushDigitalNextActionConsumer, message {}", message);
            Action action = message.getPayload();
            digitalWorkFlowHandler.startScheduledNextWorkflow(action.getIun(), action.getRecipientIndex());
        };
    }

    @Bean
    public Consumer<Message<WebhookAction>> pnDeliveryPushWebhookActionConsumer() {
        return message -> {
            log.info("pnDeliveryPushWebhookActionConsumer, message={}", message);
            WebhookAction action = message.getPayload();
            webhookActionsEventHandler.handleEvent(action);
        };
    }
}
