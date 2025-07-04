package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.input;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notificationviewed.NotificationViewedInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.EventHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.jetbrains.annotations.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@CustomLog
@AllArgsConstructor
public class NotificationViewedEventHandler implements EventHandler<PnDeliveryNotificationViewedEvent.Payload> {
    private final NotificationViewedRequestHandler notificationViewedRequestHandler;

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.NOTIFICATION_VIEWED;
    }

    @Override
    public Class<PnDeliveryNotificationViewedEvent.Payload> getPayloadType() {
        return PnDeliveryNotificationViewedEvent.Payload.class;
    }


    @Override
    public void handle(PnDeliveryNotificationViewedEvent.Payload payload, MessageHeaders headers) {
        final String processName = "NOTIFICATION VIEWED EVENT";

        try {
            log.debug("Handle message from {} with payload {} and headers {}", PnDeliveryClient.CLIENT_NAME, payload, headers);

            PnDeliveryNotificationViewedEvent pnDeliveryNewNotificationEvent = PnDeliveryNotificationViewedEvent.builder()
                    .payload(payload)
                    .header(HandleEventUtils.mapStandardEventHeader(headers))
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
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
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
