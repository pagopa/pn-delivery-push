package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.deliverypush.action.notificationpaid.NotificationPaidHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class NotificationPaidEventHandler {
    private final NotificationPaidHandler notificationPaidHandler;

    public NotificationPaidEventHandler(NotificationPaidHandler notificationPaidHandler) {
        this.notificationPaidHandler = notificationPaidHandler;
    }

    @Bean
    public Consumer<Message<PnDeliveryPaymentEvent.Payload>> pnDeliveryNotificationPaidEventConsumer() {
        return message -> {
            try {
                log.debug("Notification paid event received, message {}", message);
                log.info("pnDeliveryNotificationPaidEventConsumer begin: {}", message.getPayload());
                notificationPaidHandler.handleNotificationPaid(message.getPayload());
                log.info("pnDeliveryNotificationPaidEventConsumer exit: {}", message.getPayload());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
