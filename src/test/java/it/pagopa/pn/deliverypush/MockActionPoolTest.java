package it.pagopa.pn.deliverypush;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import org.springframework.boot.test.mock.mockito.MockBean;

public abstract class MockActionPoolTest {
    @MockBean
    private ActionsPool actionsPool;
}
