package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.ActionEvent;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WebhookBufferWriterService extends AbstractEventHandler<ActionEvent> {

    private final PnDeliveryClient pnDeliveryClient;
    private final TimelineDao timelineDao;
    private final StatusUtils statusUtils;
    private final WebhookBufferDao webhookBufferDao;
    private final WebhookConfigsDao webhookConfigsDao;

    public WebhookBufferWriterService(PnDeliveryClient pnDeliveryClient, TimelineDao timelineDao, StatusUtils statusUtils, WebhookBufferDao webhookBufferDao,
                                      WebhookConfigsDao webhookConfigsDao) {
        super(ActionEvent.class);
        this.pnDeliveryClient = pnDeliveryClient;
        this.timelineDao = timelineDao;
        this.statusUtils = statusUtils;
        this.webhookBufferDao = webhookBufferDao;
        this.webhookConfigsDao = webhookConfigsDao;
    }

    @Override
    public void handleEvent(ActionEvent evt) {
        log.info("Start WebhookBufferWriterService:handleEvent");

        String iun = evt.getHeader().getIun();

        pnDeliveryClient.getNotificationInfo(iun, false).ifPresent(notification -> {
            String paId = notification.getSender().getPaId();

            webhookConfigsDao.getWebhookInfo(paId).ifPresent(webhookConfigDto -> {

                switch (webhookConfigDto.getType()) {
                    case TIMELINE:
                        handleTimeLineTypeNotification(evt, notification, webhookConfigDto);
                        break;
                    case STATUS:
                        handleStatusTypeNotification(iun, notification, webhookConfigDto);
                        break;
                    default: {
                        log.error("Start WebhookBufferWriterService:handleEvent");
                        throw new PnInternalException("webhook type not supported");
                    }
                }
            });
        });
        log.info("End WebhookBufferWriterService:handleEvent");
    }

    private void handleTimeLineTypeNotification(ActionEvent evt, Notification notification, WebhookConfigDto webhookConfigDto) {
        timelineDao.getTimelineElement(notification.getIun(), evt.getPayload().getActionId()).ifPresent(timelineElement -> {
            String notificationElement = timelineElement.getCategory().toString();
            if (checkWriteNotification(webhookConfigDto, notificationElement)) {
                writeNewNotificationElement(notification, timelineElement.getTimestamp(), notificationElement);
            }
        });
    }

    private void handleStatusTypeNotification(String iun, Notification notification, WebhookConfigDto webhookConfigDto) {
        int numberOfRecipients = notification.getRecipients().size();
        Instant notificationCreationDate = notification.getSentAt();

        Set<TimelineElementInternal> rawTimeline = timelineDao.getTimeline(iun);

        Set<TimelineElementInternal> timelineElements = rawTimeline.stream().map(elem ->
                TimelineElement.builder()
                        .category(elem.getCategory())
                        .timestamp(elem.getTimestamp())
                        .build()
        ).collect(Collectors.toSet());

        List<NotificationStatusHistoryElement> statusHistory = statusUtils
                .getStatusHistory(timelineElements, numberOfRecipients, notificationCreationDate);

        int statusHistoryLength = statusHistory.size();
        if (statusHistoryLength >= 1) {
            NotificationStatusHistoryElement statusHistoryElement = statusHistory.get(statusHistoryLength - 1);
            String notificationElement = statusHistoryElement.getStatus().toString();
            if (checkWriteNotification(webhookConfigDto, notificationElement)) {
                writeNewNotificationElement(notification, statusHistoryElement.getActiveFrom(), notificationElement);
            }
        }
    }

    private boolean checkWriteNotification(WebhookConfigDto webhookConfigDto, String notificationElement) {
        return webhookConfigDto.isAllNotifications() || webhookConfigDto.getNotificationsElement().contains(notificationElement);
    }

    private void writeNewNotificationElement(Notification notification, Instant activeFrom, String notificationElement) {
        this.webhookBufferDao.put(
                notification.getSender().getPaId(),
                notification.getIun(),
                activeFrom,
                notificationElement
        );
    }
}
