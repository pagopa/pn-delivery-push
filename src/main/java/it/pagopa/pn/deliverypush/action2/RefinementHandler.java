package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.service.TimelineService;
import org.springframework.cloud.stream.annotation.StreamListener;

public class RefinementHandler {
    private TimelineService timelineService;

    public RefinementHandler(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @StreamListener(condition = "REFINEMENT")
    public void handleRefinement(String iun, String taxId) {
        //TODO Capire cosa altro va fatto in fase di perfezionamento
        timelineService.addRefinementToTimeline(iun, taxId);
    }
}
