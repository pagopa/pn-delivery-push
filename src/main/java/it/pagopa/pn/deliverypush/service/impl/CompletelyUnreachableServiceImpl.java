package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.commons_delivery.middleware.failednotification.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.service.CompletelyUnreachableService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CompletelyUnreachableServiceImpl implements CompletelyUnreachableService {
    private PaperNotificationFailedDao paperNotificationFailedDao;
    private TimelineService timelineService;

    public CompletelyUnreachableServiceImpl(PaperNotificationFailedDao paperNotificationFailedDao,
                                            TimelineService timelineService) {
        this.paperNotificationFailedDao = paperNotificationFailedDao;
        this.timelineService = timelineService;
    }

    public void handleCompletelyUnreachable(String iun, String taxId) {
        log.info("Start handleCompletelyUnreachable for iun {} id {} ", iun, taxId);

        if (!isNotificationAlreadyViewed(iun, taxId)) {
            addPaperNotificationFailed(iun, taxId);
        }
        timelineService.addCompletelyUnreachableToTimeline(iun, taxId);
    }

    private boolean isNotificationAlreadyViewed(String iun, String taxId) {
        //Lo user potrebbe aver visualizzato la notifica tramite canali differenti anche se non raggiunto dai canali 'legali'
        return timelineService.isPresentTimeLineElement(iun, taxId, TimelineEventId.NOTIFICATION_VIEWED);
    }

    private void addPaperNotificationFailed(String iun, String taxId) {
        log.info("AddPaperNotificationFailed for iun {} id {} ", iun, taxId);

        paperNotificationFailedDao.addPaperNotificationFailed(
                PaperNotificationFailed.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build()
        );
    }

}
