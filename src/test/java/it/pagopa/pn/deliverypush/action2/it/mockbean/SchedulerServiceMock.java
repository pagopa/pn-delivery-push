package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action2.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.RefinementHandler;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;

public class SchedulerServiceMock implements SchedulerService {
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    private AnalogWorkflowHandler analogWorkflowHandler;
    private RefinementHandler refinementHandler;

    public SchedulerServiceMock(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler, @Lazy AnalogWorkflowHandler analogWorkflowHandler, @Lazy RefinementHandler refinementHandler) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.refinementHandler = refinementHandler;
    }

    @Override
    public void scheduleEvent(String iun, String taxId, Instant dateToSchedule, ActionType actionType) {
        switch (actionType) {
            case ANALOG_WORKFLOW:
                analogWorkflowHandler.nextWorkflowStep(iun, taxId, 0);
                break;
            case REFINEMENT_NOTIFICATION:
                refinementHandler.handleRefinement(iun, taxId);
                break;
            case DIGITAL_WORKFLOW_NEXT_ACTION:
                digitalWorkFlowHandler.nextWorkFlowAction(iun, taxId);
                break;
        }
    }

}
