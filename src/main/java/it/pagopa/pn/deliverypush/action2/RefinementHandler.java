package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RefinementHandler {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public RefinementHandler(TimelineService timelineService, TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    public void handleRefinement(String iun, int recIndex) {
        log.info("Start HandleRefinement - iun {} id {}", iun, recIndex);
        addTimelineElement(timelineUtils.buildRefinementTimelineElement(iun, recIndex));
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }
}
