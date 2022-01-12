package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {
    private final ActionsPool actionsPool;

    public SchedulerServiceImpl(ActionsPool actionsPool) {
        this.actionsPool = actionsPool;
    }

    @Override
    public void scheduleEvent(String iun, String taxId, Instant dateToSchedule, ActionType actionType) {
        //TODO Da realizzare lo scheduling
        log.info("scheduleEvent date {} action {} - IUN {} and id {} ", dateToSchedule, actionType, iun, taxId);
   
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
