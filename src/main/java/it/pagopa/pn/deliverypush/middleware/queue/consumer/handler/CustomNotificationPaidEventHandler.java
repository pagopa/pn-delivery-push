package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.deliverypush.action.notificationpaid.NotificationPaidHandler;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@AllArgsConstructor
public class CustomNotificationPaidEventHandler implements EventHandler<PnDeliveryPaymentEvent.Payload> {
    private final NotificationPaidHandler notificationPaidHandler;

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.NOTIFICATION_PAID;
    }

    @Override
    public Class<PnDeliveryPaymentEvent.Payload> getPayloadType() {
        return PnDeliveryPaymentEvent.Payload.class;
    }

    @Override
    public void handle(PnDeliveryPaymentEvent.Payload paymentEventPayload, MessageHeaders headers) {
        final String processName = "NOTIFICATION PAID EVENT";

            try {
                log.debug("Handle message from {} with payload {}", PnDeliveryClient.CLIENT_NAME, paymentEventPayload);
                HandleEventUtils.addIunAndRecIndexToMdc(paymentEventPayload.getIun(), paymentEventPayload.getRecipientIdx());

                log.logStartingProcess(processName);
                notificationPaidHandler.handleNotificationPaid(paymentEventPayload);
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(headers, ex);
                throw ex;
            }
    }
}
