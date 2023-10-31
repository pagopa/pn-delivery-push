package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.MockActionPoolTest;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineCounterEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineCounterEntity;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        TimelineCounterEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        ActionDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class TimelineCounterEntityDaoDynamoTestIT extends MockActionPoolTest {
    @Autowired
    private TimelineCounterEntityDao dao;

    @Test
    void getCounter() {

        String timeline = System.currentTimeMillis()+"";
        TimelineCounterEntity r = dao.getCounter(timeline);

        Assertions.assertNotNull(r);
        Assertions.assertEquals(1L, r.getCounter());


        TimelineCounterEntity r1 = dao.getCounter(timeline);

        Assertions.assertNotNull(r1);
        Assertions.assertEquals(2L, r1.getCounter());

    }
}