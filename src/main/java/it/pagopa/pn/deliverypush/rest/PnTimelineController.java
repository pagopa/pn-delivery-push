package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineStatusHistoryDto;
import it.pagopa.pn.api.rest.PnDeliveryPushRestApi_methodGetTimeline;
import it.pagopa.pn.api.rest.PnDeliveryPushRestConstants;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

@RestController
public class PnTimelineController implements PnDeliveryPushRestApi_methodGetTimeline {

    private final TimelineService timelineService;

    public PnTimelineController(TimelineService timelineService) { this.timelineService = timelineService; }

    @Override
    @GetMapping(PnDeliveryPushRestConstants.TIMELINE_BY_IUN)
    public ResponseEntity<Set<TimelineElement>> getTimelineElements(String iun) {
        return ResponseEntity.ok().body(timelineService.getTimeline( iun ));
    }

    @Override
    @GetMapping(PnDeliveryPushRestConstants.TIMELINE_AND_STATUS_HISTORY_BY_IUN)
    public ResponseEntity<TimelineStatusHistoryDto> getTimelineAndStatusHistory(String iun, int numberOfRecipients, Instant createdAt) {
        return ResponseEntity.ok().body(timelineService.getTimelineAndStatusHistory( iun, numberOfRecipients, createdAt));
    }
    
}
