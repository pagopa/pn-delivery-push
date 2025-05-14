package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.CustomLog;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.function.Consumer;

@Configuration
@CustomLog
public class NotificationViewedEventHandler {
    private final NotificationViewedRequestHandler notificationViewedRequestHandler;

    public NotificationViewedEventHandler(NotificationViewedRequestHandler notificationViewedRequestHandler) {
        this.notificationViewedRequestHandler = notificationViewedRequestHandler;
    }

    @Bean
    public Consumer<Message<PnDeliveryNotificationViewedEvent.Payload>> pnDeliveryNotificationViewedEventConsumer() {
        final String processName = "NOTIFICATION VIEWED EVENT";

        return message -> {
            try {
                log.debug("Handle message from {} with content {}", PnDeliveryClient.CLIENT_NAME, message);

                PnDeliveryNotificationViewedEvent pnDeliveryNewNotificationEvent = PnDeliveryNotificationViewedEvent.builder()
                        .payload(message.getPayload())
                        .header(HandleEventUtils.mapStandardEventHeader(message.getHeaders()))
                        .build();

                String iun = pnDeliveryNewNotificationEvent.getHeader().getIun();
                int recipientIndex = pnDeliveryNewNotificationEvent.getPayload().getRecipientIndex();
                HandleEventUtils.addIunAndRecIndexToMdc(iun, recipientIndex);

                log.logStartingProcess(processName);

                Instant viewedDate = pnDeliveryNewNotificationEvent.getHeader().getCreatedAt();
                NotificationViewDelegateInfo delegateBasicInfo = pnDeliveryNewNotificationEvent.getPayload().getDelegateInfo();
                DelegateInfoInt delegateInfo = mapExternalToInternal(delegateBasicInfo);
                String sourceChannel = pnDeliveryNewNotificationEvent.getPayload().getSourceChannel();
                String sourceChannelDetail = pnDeliveryNewNotificationEvent.getPayload().getSourceChannelDetails();
                notificationViewedRequestHandler.handleViewNotificationDelivery(buildNotificationViewedInt(iun, recipientIndex, delegateInfo, viewedDate, sourceChannel, sourceChannelDetail));

                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
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

    private NotificationViewedInt buildNotificationViewedInt(String iun, int recipientIndex, DelegateInfoInt delegateInfo, Instant viewedDate, String sourceChannel, String sourceChannelDetail) {
        return NotificationViewedInt.builder()
                .iun(iun)
                .recipientIndex(recipientIndex)
                .delegateInfo(delegateInfo)
                .viewedDate(viewedDate)
                .sourceChannel(sourceChannel)
                .sourceChannelDetails(sourceChannelDetail)
                .build();
    }
}
