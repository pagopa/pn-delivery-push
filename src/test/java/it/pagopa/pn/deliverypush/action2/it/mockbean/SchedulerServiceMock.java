package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action2.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.RefinementHandler;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SchedulerServiceMock implements SchedulerService {
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    private AnalogWorkflowHandler analogWorkflowHandler;
    private RefinementHandler refinementHandler;
    private InstantNowSupplier instantNowSupplier;

    public SchedulerServiceMock(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler, @Lazy AnalogWorkflowHandler analogWorkflowHandler,
                                @Lazy RefinementHandler refinementHandler, @Lazy InstantNowSupplier instantNowSupplier) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.refinementHandler = refinementHandler;
        this.instantNowSupplier = instantNowSupplier;
    }

    @Override
    public void scheduleEvent(String iun, String taxId, Instant dateToSchedule, ActionType actionType) {
        /*Clock clock = Clock.fixed(dateToSchedule, ZoneId.of("UTC"));
        Instant instant = Instant.now(clock);
        try (MockedStatic<Instant> mockedStatic = mockStatic(Instant.class)) {
            mockedStatic.when(Instant::now).thenReturn(instant);
        }
        Ha bisogno di una versione di mockito aggiornata
        */
        Instant schedulingDate = dateToSchedule.plus(1, ChronoUnit.HOURS);

        Mockito.when(instantNowSupplier.get()).thenReturn(schedulingDate);

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
