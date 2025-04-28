package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.dynamo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.MockActionPoolTest;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.ActionDaoDynamo;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        ActionDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class ActionDaoDynamoTestIT extends MockActionPoolTest {
    @Autowired
    private ActionDao actionDao;

    @Test
    @ExtendWith(SpringExtension.class)
    void addOnlyActionIfAbsentFailSilent() {
        Action.ActionBuilder actionBuilder = Action.builder()
                .iun("Test_addIfAbsentFailSilent_iun01")
                .recipientIndex(1)
                .type(ActionType.DIGITAL_WORKFLOW_RETRY_ACTION);
        String actionId = ActionType.DIGITAL_WORKFLOW_NEXT_ACTION.buildActionId(
                actionBuilder.build());

        Action action = actionBuilder.actionId(actionId).build();


        // get Logback Logger
        Logger fooLogger = LoggerFactory.getLogger(ActionDaoDynamo.class);

        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        // addAppender is outdated now
        ((ch.qos.logback.classic.Logger)fooLogger).addAppender(listAppender);


        // non si riesce a mockare TransactWriteItemsEnhancedRequest
        Assertions.assertDoesNotThrow(() ->
                actionDao.addOnlyActionIfAbsent(action)
        );

        Assertions.assertDoesNotThrow(() ->
                actionDao.addOnlyActionIfAbsent(action)
        );

        // JUnit assertions
        List<ILoggingEvent> logsList = listAppender.list;
        Assertions.assertEquals("Exception code ConditionalCheckFailed is expected for retry, letting flow continue actionId=Test_addIfAbsentFailSilent_iun01_digital_workflow_e_1_timelineid_ ", logsList.get(0)
                .getFormattedMessage());
        Assertions.assertEquals(Level.WARN, logsList.get(0)
                .getLevel());
    }
}