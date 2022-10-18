package it.pagopa.pn.deliverypush.middleware;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class PnDeliveryPushMiddlewareConfigsTest {

    @Mock
    private PnDeliveryPushConfigs cfg;

    private PnDeliveryPushMiddlewareConfigs configs;

    @BeforeEach
    void setUp() {
        cfg = Mockito.mock(PnDeliveryPushConfigs.class);
        configs = new PnDeliveryPushMiddlewareConfigs(cfg);
    }

    @Test
    void pecRequestSender() {
    }

    @Test
    void emailRequestSender() {
    }

    @Test
    void paperRequestSender() {
    }

    @Test
    void actionsEventProducer() {
    }

    @Test
    void actionsDoneEventProducer() {
    }

    @Test
    void webhookActionsEventProducer() {
    }
}