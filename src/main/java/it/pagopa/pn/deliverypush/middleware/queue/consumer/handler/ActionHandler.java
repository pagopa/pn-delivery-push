package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;


import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.EventRouter;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ACTIONTYPENOTSUPPORTED;


@Configuration
@AllArgsConstructor
@CustomLog
public class ActionHandler {
    private final EventRouter eventRouter;

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushValidationActionsInboundConsumer() {
        final String processName = "VALIDATION_ACTIONS_INBOUND";

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushValidationActionsInboundConsumer, with content {}", message);
                String actionType = extractActionType(message.getPayload());

                EventRouter.RoutingConfig routerConfig = EventRouter.RoutingConfig.builder()
                        .eventType(actionType)
                        .build();
                eventRouter.route(message, routerConfig);
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushActionsInboundConsumer() {
        final String processName = "WORKFLOW_ACTIONS_INBOUND";

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushActionsInboundConsumer, with content {}", message);
                String actionType = extractActionType(message.getPayload());


                EventRouter.RoutingConfig routerConfig = EventRouter.RoutingConfig.builder()
                        .eventType(actionType)
                        .build();
                eventRouter.route(message, routerConfig);
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    private String extractActionType(Action action) {
        String actionType = action.getType() != null ? action.getType().name() : null;
        if (actionType == null) {
            log.error("actionType not present, cannot start scheduled action");
            throw new PnInternalException("actionType not present, cannot start scheduled action", ERROR_CODE_DELIVERYPUSH_ACTIONTYPENOTSUPPORTED);
        }

        return actionType;
    }

}
