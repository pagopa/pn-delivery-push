package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.service.ActionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ActionServiceImpl implements ActionService {
    private final ActionDao actionDao;

    public ActionServiceImpl(ActionDao actionDao) {
        this.actionDao = actionDao;
    }

    @Override
    public void addActionAndFutureActionIfAbsent(Action action, String timeSlot) {
        actionDao.addActionAndFutureActionIfAbsent(action, timeSlot);
    }

    @Override
    public void addOnlyActionIfAbsent(Action action) {
         actionDao.addOnlyActionIfAbsent(action);
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
        actionDao.unSchedule(action, timeSlot);
    }
}
