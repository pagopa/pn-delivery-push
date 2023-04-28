package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.papernotificationfailed.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CompletelyUnreachableUtils  {
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;

    public CompletelyUnreachableUtils(PaperNotificationFailedService paperNotificationFailedService,
                                      TimelineService timelineService,
                                      TimelineUtils timelineUtils,
                                      NotificationUtils notificationUtils) {
        this.paperNotificationFailedService = paperNotificationFailedService;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.notificationUtils = notificationUtils;
    }

    public void handleCompletelyUnreachable(NotificationInt notification, Integer recIndex) {
        log.info("HandleCompletelyUnreachable - iun {} id {} ", notification.getIun(), recIndex);
        boolean isNotificationViewedOrPaid = timelineUtils.checkNotificationIsViewedOrPaid(notification.getIun(), recIndex);

        if (!isNotificationViewedOrPaid) {
            addPaperNotificationFailed(notification, recIndex);
        }
        addTimelineElement( 
                timelineUtils.buildCompletelyUnreachableTimelineElement(notification, recIndex),
                notification);
    }

    private void addPaperNotificationFailed(NotificationInt notification, Integer recIndex) {
        log.info("AddPaperNotificationFailed - iun {} id {} ", notification.getIun(), recIndex);
        
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
        
        paperNotificationFailedService.addPaperNotificationFailed(
                PaperNotificationFailed.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getInternalId())
                        .build()
        );
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
