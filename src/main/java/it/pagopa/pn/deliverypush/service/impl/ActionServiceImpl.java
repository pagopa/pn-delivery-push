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
    public void addOnlyActionIfAbsent(Action action) {
         actionDao.addOnlyActionIfAbsent(action);
    }
}
