package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.springframework.cloud.stream.annotation.StreamListener;

public class RefinementHandler {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public RefinementHandler(TimelineService timelineService, TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    @StreamListener(condition = "REFINEMENT")
    public void handleRefinement(String iun, String taxId) {
        //TODO Capire cosa altro va fatto in fase di perfezionamento
        addTimelineElement(timelineUtils.buildRefinementTimelineElement(iun, taxId));
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }
}
