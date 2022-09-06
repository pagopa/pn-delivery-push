package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnExtRegistryNotificationPaidEvent;
import it.pagopa.pn.deliverypush.action.NotificationPaidHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class NotificationPaidEventHandler {
    private final NotificationPaidHandler notificationPaidHandler;

    public NotificationPaidEventHandler(NotificationPaidHandler notificationPaidHandler) {
        this.notificationPaidHandler = notificationPaidHandler;
    }

    @Bean
    public Consumer<Message<PnExtRegistryNotificationPaidEvent.Payload>> pnExtRegistryNotificationPaidEventConsumer() {
        return message -> {
            try {
                log.debug("Notification paid event received, message {}", message);

                PnExtRegistryNotificationPaidEvent notificationPaidEvent = PnExtRegistryNotificationPaidEvent.builder()
                        .payload(message.getPayload())
                        .header(HandleEventUtils.mapStandardEventHeader(message.getHeaders()))
                        .build();
                
                Instant eventDate = notificationPaidEvent.getPayload().getEventDate();
                String noticeCode = notificationPaidEvent.getPayload().getNoticeCode();
                String paTaxId =notificationPaidEvent.getPayload().getPaTaxId();
                
                log.info("pnExtRegistryNotificationPaidEventConsumer - eventDate={} noticeCode={} paTaxId={}", eventDate, noticeCode, paTaxId);

                notificationPaidHandler.handleNotificationPaid(paTaxId, noticeCode, eventDate);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
