package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl.WebhookActionsEventHandler;
import it.pagopa.pn.deliverypush.action.*;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class ActionHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final RefinementHandler refinementHandler;
    private final WebhookActionsEventHandler webhookActionsEventHandler;
    private final StartWorkflowForRecipientHandler startWorkflowForRecipientHandler;
    private final ChooseDeliveryModeHandler chooseDeliveryModeHandler;
    
    public ActionHandler(DigitalWorkFlowHandler digitalWorkFlowHandler,
                         DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler, AnalogWorkflowHandler analogWorkflowHandler,
                         RefinementHandler refinementHandler,
                         WebhookActionsEventHandler webhookActionsEventHandler,
                         StartWorkflowForRecipientHandler startWorkflowForRecipientHandler,
                         ChooseDeliveryModeHandler chooseDeliveryModeHandler) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.digitalWorkFlowRetryHandler = digitalWorkFlowRetryHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.refinementHandler = refinementHandler;
        this.webhookActionsEventHandler = webhookActionsEventHandler;
        this.startWorkflowForRecipientHandler = startWorkflowForRecipientHandler;
        this.chooseDeliveryModeHandler = chooseDeliveryModeHandler;
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushStartRecipientWorkflow() {
        return message -> {
            try {
                log.debug("pnDeliveryPushStartRecipientWorkflow, message {}", message);
                Action action = message.getPayload();
                startWorkflowForRecipientHandler.startNotificationWorkflowForRecipient(action.getIun(), action.getRecipientIndex(), (String) action.getDetails().get("quickAccessLinkToken"));
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushChooseDeliveryMode() {
        return message -> {
            try {
                log.debug("pnDeliveryPushChooseDeliveryMode, message {}", message);
                Action action = message.getPayload();
                chooseDeliveryModeHandler.chooseDeliveryTypeAndStartWorkflow(action.getIun(), action.getRecipientIndex());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
    
    @Bean
    public Consumer<Message<Action>> pnDeliveryPushAnalogWorkflowConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushAnalogWorkflowConsumer, message {}", message);
                Action action = message.getPayload();
                analogWorkflowHandler.startAnalogWorkflow(action.getIun(), action.getRecipientIndex());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushRefinementConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushRefinementConsumer, message {}", message);
                Action action = message.getPayload();
                refinementHandler.handleRefinement(action.getIun(), action.getRecipientIndex());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalNextActionConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushDigitalNextActionConsumer, message {}", message);
                Action action = message.getPayload();
                digitalWorkFlowHandler.startScheduledNextWorkflow(action.getIun(), action.getRecipientIndex());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }


    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalRetryActionConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushDigitalRetryActionConsumer, message {}", message);
                Action action = message.getPayload();
                digitalWorkFlowRetryHandler.startScheduledRetryWorkflow(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer, message {}", message);
                Action action = message.getPayload();
                digitalWorkFlowRetryHandler.elapsedExtChannelTimeout(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }



    @Bean
    public Consumer<Message<WebhookAction>> pnDeliveryPushWebhookActionConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushWebhookActionConsumer, message={}", message);
                WebhookAction action = message.getPayload();
                webhookActionsEventHandler.handleEvent(action);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
