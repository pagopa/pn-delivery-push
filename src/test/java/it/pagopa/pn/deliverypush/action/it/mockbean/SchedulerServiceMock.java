package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.action.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.RefinementHandler;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SchedulerServiceMock implements SchedulerService {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final RefinementHandler refinementHandler;
    private final InstantNowSupplier instantNowSupplier;

    public SchedulerServiceMock(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler, @Lazy AnalogWorkflowHandler analogWorkflowHandler,
                                @Lazy RefinementHandler refinementHandler, @Lazy InstantNowSupplier instantNowSupplier) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.refinementHandler = refinementHandler;
        this.instantNowSupplier = instantNowSupplier;
    }

    @Override
    public void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType) {
        mockSchedulingDate(dateToSchedule);

        switch (actionType) {
            case ANALOG_WORKFLOW:
                analogWorkflowHandler.startAnalogWorkflow(iun, recIndex);
                break;
            case REFINEMENT_NOTIFICATION:
                refinementHandler.handleRefinement(iun, recIndex);
                break;
            case DIGITAL_WORKFLOW_NEXT_ACTION:
                digitalWorkFlowHandler.startScheduledNextWorkflow(iun, recIndex);
                break;
        }
    }

    @Override
    public void scheduleWebhookEvent(String paId, String iun, String timelineId, Instant timestamp, String oldStatus, String newStatus, String timelineEventCategory) {
        // non usato come mock
    }

    @Override
    public void scheduleWebhookEvent(String streamId, String eventId, Integer delay, WebhookEventType actionType) {
        // non usato come mock
    }

    private void mockSchedulingDate(Instant dateToSchedule) {
        Instant schedulingDate = dateToSchedule.plus(1, ChronoUnit.HOURS);
        Mockito.when(instantNowSupplier.get()).thenReturn(schedulingDate);
    }

}
