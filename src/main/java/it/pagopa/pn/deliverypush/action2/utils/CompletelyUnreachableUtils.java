package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.commons_delivery.middleware.failednotification.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CompletelyUnreachableUtils {
    private final PaperNotificationFailedDao paperNotificationFailedDao;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public CompletelyUnreachableUtils(PaperNotificationFailedDao paperNotificationFailedDao, TimelineService timelineService,
                                      TimelineUtils timelineUtils) {
        this.paperNotificationFailedDao = paperNotificationFailedDao;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    public void handleCompletelyUnreachable(String iun, String taxId) {
        log.info("HandleCompletelyUnreachable - iun {} id {} ", iun, taxId);

        if (!isNotificationAlreadyViewed(iun, taxId)) {
            addPaperNotificationFailed(iun, taxId);
        }
        addTimelineElement(timelineUtils.buildCompletelyUnreachableTimelineElement(iun, taxId));
    }

    private boolean isNotificationAlreadyViewed(String iun, String taxId) {
        //Lo user potrebbe aver visualizzato la notifica tramite canali differenti anche se non raggiunto dai canali 'legali'
        return timelineService.isPresentTimeLineElement(iun, taxId, TimelineEventId.NOTIFICATION_VIEWED);
    }

    private void addPaperNotificationFailed(String iun, String taxId) {
        log.info("AddPaperNotificationFailed - iun {} id {} ", iun, taxId);

        paperNotificationFailedDao.addPaperNotificationFailed(
                PaperNotificationFailed.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build()
        );
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }
}
