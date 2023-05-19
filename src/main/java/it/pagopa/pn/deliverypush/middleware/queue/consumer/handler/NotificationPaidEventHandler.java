package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.deliverypush.action.notificationpaid.NotificationPaidHandler;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@CustomLog
public class NotificationPaidEventHandler {
    private final NotificationPaidHandler notificationPaidHandler;

    public NotificationPaidEventHandler(NotificationPaidHandler notificationPaidHandler) {
        this.notificationPaidHandler = notificationPaidHandler;
    }

    @Bean
    public Consumer<Message<PnDeliveryPaymentEvent.Payload>> pnDeliveryNotificationPaidEventConsumer() {
        final String processName = "NOTIFICATION PAID EVENT";

        return message -> {
            try {
                log.debug("Handle message from {} with content {}", PnDeliveryClient.CLIENT_NAME, message);
                PnDeliveryPaymentEvent.Payload paymentEventPayload = message.getPayload();
                HandleEventUtils.addIunAndRecIndexToMdc(paymentEventPayload.getIun(), paymentEventPayload.getRecipientIdx());

                log.logStartingProcess(processName);
                notificationPaidHandler.handleNotificationPaid(message.getPayload());
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
