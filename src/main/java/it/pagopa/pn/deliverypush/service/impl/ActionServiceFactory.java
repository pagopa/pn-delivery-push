package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager.ActionManagerClient;
import it.pagopa.pn.deliverypush.service.ActionService;
import it.pagopa.pn.deliverypush.service.mapper.ActionManagerMapper;
import it.pagopa.pn.deliverypush.service.utils.FeatureFlagUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
@Slf4j
public class ActionServiceFactory {
    private final ActionDao actionDao;
    private final ActionManagerClient actionManagerClient;
    private final ActionManagerMapper actionManagerMapper;
    private final FeatureFlagUtils featureFlagUtils;

    /**
     * Factory method to get the appropriate ActionService implementation based on feature flags.
     * This method checks the current time against the feature flag configuration
     * to determine whether to return the DAO implementation or the REST implementation.
     * @return ActionService instance, either ActionServiceImpl or ActionServiceRestImpl
     */
    public ActionService getActionService() {
        Instant now = Instant.now();
        log.info("Current time for ActionService implementation check: {}", now);
        if(featureFlagUtils.isActionImplDao(now)) {
            log.debug("Using DAO implementation for ActionService");
            return new ActionServiceImpl(actionDao);
        } else {
            log.debug("Using REST implementation for ActionService");
            return new ActionServiceRestImpl(actionManagerClient, actionManagerMapper);
        }
    }
}
