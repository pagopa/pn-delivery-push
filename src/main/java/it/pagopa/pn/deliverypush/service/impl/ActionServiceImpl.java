package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.service.ActionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class ActionServiceImpl implements ActionService {
    private final ActionDao actionDao;

    @Override
    public void addOnlyActionIfAbsent(Action action) {
         actionDao.addOnlyActionIfAbsent(action);
    }

    @Override
    public Optional<Action> getActionById(String actionId) {
        return actionDao.getActionById(actionId);
    }

    @Override
    public void unSchedule(Action action, String timeSlot) {
        actionDao.unScheduleFutureAction(action, timeSlot);
    }
}
