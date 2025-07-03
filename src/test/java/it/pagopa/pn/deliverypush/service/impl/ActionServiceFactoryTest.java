package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager.ActionManagerClient;
import it.pagopa.pn.deliverypush.service.ActionService;
import it.pagopa.pn.deliverypush.service.mapper.ActionManagerMapper;
import it.pagopa.pn.deliverypush.service.utils.FeatureFlagUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ActionServiceFactoryTest {

    private FeatureFlagUtils featureFlagUtils;
    private ActionServiceFactory factory;

    @BeforeEach
    void setUp() {
        ActionDao actionDao = Mockito.mock(ActionDao.class);
        ActionManagerClient actionManagerClient = Mockito.mock(ActionManagerClient.class);
        ActionManagerMapper actionManagerMapper = Mockito.mock(ActionManagerMapper.class);
        featureFlagUtils = Mockito.mock(FeatureFlagUtils.class);
        factory = new ActionServiceFactory(actionDao, actionManagerClient, actionManagerMapper, featureFlagUtils);
    }

    @Test
    void getActionService_returnsDaoImplementationWhenFeatureFlagIsTrue() {
        Mockito.when(featureFlagUtils.isActionImplDao(Mockito.any(Instant.class))).thenReturn(true);

        ActionService service = factory.getActionService();

        assertNotNull(service);
        assertEquals("ActionServiceImpl", service.getClass().getSimpleName());
    }

    @Test
    void getActionService_returnsRestImplementationWhenFeatureFlagIsFalse() {
        Mockito.when(featureFlagUtils.isActionImplDao(Mockito.any(Instant.class))).thenReturn(false);

        ActionService service = factory.getActionService();

        assertNotNull(service);
        assertEquals("ActionServiceRestImpl", service.getClass().getSimpleName());
    }
}