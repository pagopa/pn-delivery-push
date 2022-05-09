package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.dto.notification.timeline.NotificationHistoryResponse;
import it.pagopa.pn.api.rest.PnDeliveryPushRestApi_methodGetTimeline;
import it.pagopa.pn.api.rest.PnDeliveryPushRestConstants;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Slf4j
@RestController
public class PnTimelineController implements PnDeliveryPushRestApi_methodGetTimeline {

    private final TimelineService timelineService;

    public PnTimelineController(TimelineService timelineService) { this.timelineService = timelineService; }
    
    @Override
    @GetMapping(PnDeliveryPushRestConstants.TIMELINE_AND_STATUS_HISTORY_BY_IUN)
    public ResponseEntity<NotificationHistoryResponse> getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        log.debug("Received request getTimelineAndStatusHistory - iun {} numberOfRecipients {} createdAt {}", iun, numberOfRecipients, createdAt);
        return ResponseEntity.ok().body(timelineService.getTimelineAndStatusHistory( iun, numberOfRecipients, createdAt));
    }
    
}
