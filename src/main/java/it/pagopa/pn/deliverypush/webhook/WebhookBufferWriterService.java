package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.webhook.WebhookConfigDto;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.DateUtils;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.ActionEvent;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElement;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.temp.mom.consumer.AbstractEventHandler;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class WebhookBufferWriterService extends AbstractEventHandler<ActionEvent> {

    private final NotificationService notificationService;
    private final TimelineDao timelineDao;
    private final StatusUtils statusUtils;
    private final WebhookBufferDao webhookBufferDao;
    private final WebhookConfigsDao webhookConfigsDao;

    public WebhookBufferWriterService(NotificationService notificationService, TimelineDao timelineDao, StatusUtils statusUtils, WebhookBufferDao webhookBufferDao,
                                      WebhookConfigsDao webhookConfigsDao) {
        super(ActionEvent.class);
        this.notificationService = notificationService;
        this.timelineDao = timelineDao;
        this.statusUtils = statusUtils;
        this.webhookBufferDao = webhookBufferDao;
        this.webhookConfigsDao = webhookConfigsDao;
    }

    @Override
    public void handleEvent(ActionEvent evt) {
        log.info("Start WebhookBufferWriterService:handleEvent");

        String iun = evt.getHeader().getIun();
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        
        String paId = notification.getPaNotificationId();

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
        
        log.info("End WebhookBufferWriterService:handleEvent");
    }

    private void handleTimeLineTypeNotification(ActionEvent evt, NotificationInt notification, WebhookConfigDto webhookConfigDto) {
        timelineDao.getTimelineElement(notification.getIun(), evt.getPayload().getActionId()).ifPresent(timelineElement -> {
            String notificationElement = timelineElement.getCategory().toString();
            if (checkWriteNotification(webhookConfigDto, notificationElement)) {
                writeNewNotificationElement(notification, DateUtils.convertDateToInstant(timelineElement.getTimestamp()), notificationElement);
            }
        });
    }

    private void handleStatusTypeNotification(String iun, NotificationInt notification, WebhookConfigDto webhookConfigDto) {
        int numberOfRecipients = notification.getRecipients().size();
        Instant notificationCreationDate = notification.getSentAt();

        Set<TimelineElementInternal> timelineElements = timelineDao.getTimeline(iun);

        List<NotificationStatusHistoryElement> statusHistory = statusUtils
                .getStatusHistory(timelineElements, numberOfRecipients, notificationCreationDate);

        int statusHistoryLength = statusHistory.size();
        if (statusHistoryLength >= 1) {
            NotificationStatusHistoryElement statusHistoryElement = statusHistory.get(statusHistoryLength - 1);
            String notificationElement = statusHistoryElement.getStatus().toString();
            if (checkWriteNotification(webhookConfigDto, notificationElement)) {
                writeNewNotificationElement(notification, DateUtils.convertDateToInstant(statusHistoryElement.getActiveFrom()), notificationElement);
            }
        }
    }

    private boolean checkWriteNotification(WebhookConfigDto webhookConfigDto, String notificationElement) {
        return webhookConfigDto.isAllNotifications() || webhookConfigDto.getNotificationsElement().contains(notificationElement);
    }

    private void writeNewNotificationElement(NotificationInt notification, Instant activeFrom, String notificationElement) {
        this.webhookBufferDao.put(
                notification.getSender().getPaId(),
                notification.getIun(),
                activeFrom,
                notificationElement
        );
    }
}
