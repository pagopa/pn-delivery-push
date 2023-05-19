package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class NotificationViewedEventHandler {
    private final NotificationViewedRequestHandler notificationViewedRequestHandler;

    public NotificationViewedEventHandler(NotificationViewedRequestHandler notificationViewedRequestHandler) {
        this.notificationViewedRequestHandler = notificationViewedRequestHandler;
    }

    @Bean
    public Consumer<Message<PnDeliveryNotificationViewedEvent.Payload>> pnDeliveryNotificationViewedEventConsumer() {
        return message -> {
            try {
                log.debug("Handle message from {} with content {}", PnDeliveryClient.CLIENT_NAME, message);

                PnDeliveryNotificationViewedEvent pnDeliveryNewNotificationEvent = PnDeliveryNotificationViewedEvent.builder()
                        .payload(message.getPayload())
                        .header(HandleEventUtils.mapStandardEventHeader(message.getHeaders()))
                        .build();

                String iun = pnDeliveryNewNotificationEvent.getHeader().getIun();
                int recipientIndex = pnDeliveryNewNotificationEvent.getPayload().getRecipientIndex();
                Instant viewedDate = pnDeliveryNewNotificationEvent.getHeader().getCreatedAt();
                NotificationViewDelegateInfo delegateBasicInfo = pnDeliveryNewNotificationEvent.getPayload().getDelegateInfo();
                log.info("pnDeliveryNotificationViewedEventConsumer - iun {} id={} delegateBasicInfo={} viewedDate={}", iun, recipientIndex, viewedDate, delegateBasicInfo);

                DelegateInfoInt delegateInfo = mapExternalToInternal(delegateBasicInfo);
                notificationViewedRequestHandler.handleViewNotificationDelivery(iun, recipientIndex, delegateInfo, viewedDate);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Nullable
    private DelegateInfoInt mapExternalToInternal(NotificationViewDelegateInfo notificationViewDelegateInfo) {
        DelegateInfoInt delegateInfo = null;

        if(notificationViewDelegateInfo != null){
            delegateInfo = DelegateInfoInt.builder()
                    .internalId(notificationViewDelegateInfo.getInternalId())
                    .operatorUuid(notificationViewDelegateInfo.getOperatorUuid())
                    .mandateId(notificationViewDelegateInfo.getMandateId())
                    .delegateType(RecipientTypeInt.valueOf(notificationViewDelegateInfo.getDelegateType().getValue()))
                    .build();
        }

        return delegateInfo;
    }
}
