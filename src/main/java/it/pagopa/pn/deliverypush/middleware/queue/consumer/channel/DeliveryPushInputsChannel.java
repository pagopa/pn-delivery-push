package it.pagopa.pn.deliverypush.middleware.queue.consumer.channel;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.EventRouter;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@CustomLog
public class DeliveryPushInputsChannel {
    private final EventRouter eventRouter;

    @Bean
    public Consumer<Message<String>> pnDeliveryPushInputsInboundConsumer() {

        final String processName = "DELIVERY_PUSH_INPUTS_INBOUND_CONSUMER";

        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushInputsInboundConsumer, with content {}", message);


                EventRouter.RoutingConfig routerConfig = EventRouter.RoutingConfig.builder()
                        .deserializePayload(true)
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
}
