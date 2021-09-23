package it.pagopa.pn.deliverypush.webhook;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
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

    public WebhookBufferWriterService(NotificationDao notificationDao, TimelineDao timelineDao, StatusUtils statusUtils, WebhookBufferDao webhookBufferDao) {
        super( ActionEvent.class );
        this.notificationDao = notificationDao;
        this.timelineDao = timelineDao;
        this.statusUtils = statusUtils;
        this.webhookBufferDao = webhookBufferDao;
    }

    @Override
    public void handleEvent(ActionEvent evt ) {
        String iun = evt.getHeader().getIun();

        notificationDao.getNotificationByIun( iun ).ifPresent( notification -> {
            int numberOfRecipients = notification.getRecipients().size();
            Instant notificationCreationDate = notification.getSentAt();

            Set<TimelineElement> rawTimeline = timelineDao.getTimeline( iun );

            List<NotificationStatusHistoryElement> statusHistory = statusUtils
                    .getStatusHistory( rawTimeline, numberOfRecipients, notificationCreationDate );

            int statusHistoryLength = statusHistory.size();
            if(  statusHistoryLength >= 1 ) {
                writeNewStatus( notification, statusHistory.get( statusHistoryLength - 1 ));
            }
        });
    }

    private void writeNewStatus( Notification notification, NotificationStatusHistoryElement statusChange) {
        this.webhookBufferDao.put(
                notification.getSender().getPaId(),
                notification.getIun(),
                statusChange.getActiveFrom(),
                statusChange.getStatus()
            );
    }

}
