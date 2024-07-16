package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.service.ActionService;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class ActionServiceImpl implements ActionService {
    private final ActionDao actionDao;
    private final FeatureEnabledUtils featureEnabledUtils;

    @Override
    public void addActionAndFutureActionIfAbsent(Action action, String timeSlot) {
        actionDao.addActionAndFutureActionIfAbsent(action, timeSlot);
    }

    @Override
    public void addOnlyActionIfAbsent(Action action) {
         actionDao.addOnlyActionIfAbsent(action);
    }

    @Override
    public void addOnlyAction(Action action) {
        actionDao.addOnlyAction(action);
    }

    @Override
    public Optional<Action> getActionById(String actionId) {
        return actionDao.getActionById(actionId);
    }

    @Override
    public List<Action> findActionsByTimeSlot(String timeSlot) {
        return actionDao.findActionsByTimeSlot(timeSlot);
    }

    @Override
    public void unSchedule(Action action, String timeSlot) {
        if(featureEnabledUtils.isPerformanceImprovementEnabled(action.getNotBefore())) {
            log.debug("Performance improvement is enabled not need to unschedule futureAction - actionId={}", action.getActionId());
        }else {
            log.debug("Performance improvement disabled need to unschedule futureAction - actionId={}", action.getActionId());
            actionDao.unSchedule(action, timeSlot);
        }
    }
}
