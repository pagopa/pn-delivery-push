package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SchedulerServiceimpl implements SchedulerService {
    private final ActionsPool actionsPool;

    public SchedulerServiceimpl(ActionsPool actionsPool) {
        this.actionsPool = actionsPool;
    }

    @Override
    public void scheduleEvent(String iun, String taxId, Instant dateToSchedule, ActionType actionType) {
        //TODO Da realizzare lo scheduling
            
        /*
        Action actionToSchedule = Action.builder()
                .iun(iun)
                .recipientIndex(taxId)
                .notBefore(dateToSchedule)
                .type(actionType).build();

        this.actionsPool.scheduleFutureAction(actionToSchedule.toBuilder()
                .actionId(actionToSchedule.getType().buildActionId(actionToSchedule))
                .build()
        );*/
    }
}
