package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.service.ActionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
@ConditionalOnProperty(name = ActionService.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = "IMPL", matchIfMissing = true)
public class ActionServiceImpl implements ActionService {
    private final ActionDao actionDao;

    @Override
    public void addOnlyActionIfAbsent(Action action) {
         actionDao.addOnlyActionIfAbsent(action);
    }

    @Override
    public void unSchedule(String actionId) {
        actionDao.unScheduleFutureAction(actionId);
    }
}
