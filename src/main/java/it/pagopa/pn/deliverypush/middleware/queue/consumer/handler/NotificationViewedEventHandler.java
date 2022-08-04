package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.deliverypush.action.NotificationViewedHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class NotificationViewedEventHandler {
    private final NotificationViewedHandler notificationViewedHandler;

    public NotificationViewedEventHandler(NotificationViewedHandler notificationViewedHandler) {
        this.notificationViewedHandler = notificationViewedHandler;
    }

    @Bean
    public Consumer<Message<PnDeliveryNotificationViewedEvent.Payload>> pnDeliveryNotificationViewedEventConsumer() {
        return message -> {
            try {
                log.debug("Notification viewed event received, message {}", message);

                PnDeliveryNotificationViewedEvent pnDeliveryNewNotificationEvent = PnDeliveryNotificationViewedEvent.builder()
                        .payload(message.getPayload())
                        .header(HandleEventUtils.mapStandardEventHeader(message.getHeaders()))
                        .build();

                String iun = pnDeliveryNewNotificationEvent.getHeader().getIun();
                int recipientIndex = pnDeliveryNewNotificationEvent.getPayload().getRecipientIndex();
                log.info("pnDeliveryNotificationViewedEventConsumer - iun {}", iun);

                notificationViewedHandler.handleViewNotification(iun, recipientIndex);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
