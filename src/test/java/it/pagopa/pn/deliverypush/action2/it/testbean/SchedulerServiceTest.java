package it.pagopa.pn.deliverypush.action2.it.testbean;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action2.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.RefinementHandler;
import it.pagopa.pn.deliverypush.service.SchedulerService;

import java.time.Instant;

public class SchedulerServiceTest implements SchedulerService {
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    private AnalogWorkflowHandler analogWorkflowHandler;
    private RefinementHandler refinementHandler;

    public SchedulerServiceTest(DigitalWorkFlowHandler digitalWorkFlowHandler, AnalogWorkflowHandler analogWorkflowHandler,
                                RefinementHandler refinementHandler) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.refinementHandler = refinementHandler;
    }

    @Override
    public void scheduleEvent(String iun, String taxId, Instant dateToSchedule, ActionType actionType) {
        switch (actionType) {
            case ANALOG_WORKFLOW:
                analogWorkflowHandler.nextWorkflowStep(iun, taxId);
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
