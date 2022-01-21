package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action2.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.RefinementHandler;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final RefinementHandler refinementHandler;
    private final InstantNowSupplier instantNowSupplier;

    public SchedulerServiceImpl(@Lazy DigitalWorkFlowHandler digitalWorkFlowHandler, @Lazy AnalogWorkflowHandler analogWorkflowHandler,
                                @Lazy RefinementHandler refinementHandler, @Lazy InstantNowSupplier instantNowSupplier) {
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.refinementHandler = refinementHandler;
        this.instantNowSupplier = instantNowSupplier;
    }

    @Override
    public void scheduleEvent(String iun, String taxId, Instant dateToSchedule, ActionType actionType) {
        //TODO Da realizzare lo scheduling
        log.info("scheduleEvent date {} action {} - IUN {} and id {} ", dateToSchedule, actionType, iun, taxId);

        Duration res = Duration.between(Instant.now(), dateToSchedule);

        if (!res.isNegative()) {
            try {
                Thread.sleep(res.toMillis() + 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        switch (actionType) {
            case ANALOG_WORKFLOW:
                analogWorkflowHandler.nextWorkflowStep(iun, taxId, 0);
                break;
            case REFINEMENT_NOTIFICATION:
                refinementHandler.handleRefinement(iun, taxId);
                break;
            case DIGITAL_WORKFLOW_NEXT_ACTION:
                digitalWorkFlowHandler.startScheduledNextWorkflow(iun, taxId);
                break;
        }
    }

}
