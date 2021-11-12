package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.ActionEvent;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class WebhookBufferWriterService extends AbstractEventHandler<ActionEvent> {

    private final NotificationDao notificationDao;
    private final TimelineDao timelineDao;
    private final StatusUtils statusUtils;
    private final WebhookBufferDao webhookBufferDao;
    private final WebhookConfigsDao webhookConfigsDao;

    public WebhookBufferWriterService(NotificationDao notificationDao, TimelineDao timelineDao, StatusUtils statusUtils, WebhookBufferDao webhookBufferDao,
                                      WebhookConfigsDao webhookConfigsDao) {
        super(ActionEvent.class);
        this.notificationDao = notificationDao;
        this.timelineDao = timelineDao;
        this.statusUtils = statusUtils;
        this.webhookBufferDao = webhookBufferDao;
        this.webhookConfigsDao = webhookConfigsDao;
    }

    @Override
    public void handleEvent(ActionEvent evt) {
        log.info("Start WebhookBufferWriterService:handleEvent");

        String iun = evt.getHeader().getIun();

        notificationDao.getNotificationByIun(iun).ifPresent(notification -> {
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

        Set<TimelineElement> rawTimeline = timelineDao.getTimeline(iun);

        List<NotificationStatusHistoryElement> statusHistory = statusUtils
                .getStatusHistory(rawTimeline, numberOfRecipients, notificationCreationDate);

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
