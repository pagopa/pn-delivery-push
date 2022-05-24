package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
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
    public void addAction(Action action, String timeSlot) {
        actionDao.addAction(action, timeSlot);
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
