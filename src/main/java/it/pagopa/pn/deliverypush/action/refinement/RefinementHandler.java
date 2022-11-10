package it.pagopa.pn.deliverypush.action.refinement;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationCostService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RefinementHandler {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;
    private final NotificationCostService notificationCostService;
    
    public RefinementHandler(TimelineService timelineService,
                             TimelineUtils timelineUtils,
                             NotificationService notificationService, 
                             NotificationCostService notificationCostService) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.notificationService = notificationService;
        this.notificationCostService = notificationCostService;
    }

    public void handleRefinement(String iun, Integer recIndex) {
        log.info("Start HandleRefinement - iun {} id {}", iun, recIndex);
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(iun, recIndex);

        //Se la notifica è già stata visualizzata non viene perfezionata per decorrenza termini in quanto è già stata perfezionata per presa visione
        if( !isNotificationAlreadyViewed ){
            log.info("Handle refinement - iun {} id {}", iun, recIndex);
            NotificationInt notification = notificationService.getNotificationByIun(iun);
            Integer notificationCost = notificationCostService.getNotificationCost(notification, recIndex);
            log.debug("Notification cost is {} - iun {} id {}",notificationCost, iun, recIndex);

            addTimelineElement(
                    timelineUtils.buildRefinementTimelineElement(notification, recIndex, notificationCost),
                    notification
            );
        }else {
            log.info("Notification is already viewed, refinement will not start - iun={} id={}", iun, recIndex);
        }
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
