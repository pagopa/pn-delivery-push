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
        ((ch.qos.logback.classic.Logger) fooLogger).addAppender(listAppender);


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

    @Test
    void unScheduleFutureAction_actionNotFound_shouldLogInfo() {
        String actionIdFirst = "not_existing_action_id";

        Logger logger = LoggerFactory.getLogger(ActionDaoDynamo.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        ((ch.qos.logback.classic.Logger) logger).addAppender(listAppender);

        // Caso: action non trovata
        Assertions.assertDoesNotThrow(() ->
                actionDao.unScheduleFutureAction(actionIdFirst)
        );

        List<ILoggingEvent> logsList1 = listAppender.list;
        Assertions.assertTrue(
                logsList1.stream().anyMatch(
                        log -> log.getFormattedMessage().contains("Action with id " + actionIdFirst + " not found, cannot update logical deleted")
                )
        );

        Action.ActionBuilder actionBuilder = Action.builder()
                .iun("Test_unscheduleFutureAction_iun02")
                .recipientIndex(1)
                .type(ActionType.REFINEMENT_NOTIFICATION)
                .timeslot("2024-06-01T10:00");
        String actionId = ActionType.REFINEMENT_NOTIFICATION.buildActionId(actionBuilder.build());
        Action action = actionBuilder.actionId(actionId).build();

        actionDao.addOnlyActionIfAbsent(action);

        Assertions.assertDoesNotThrow(() ->
                actionDao.unScheduleFutureAction(action.getActionId())
        );

        List<ILoggingEvent> logsList = listAppender.list;
        Assertions.assertTrue(
                logsList.stream().anyMatch(
                        log -> log.getFormattedMessage().contains("Exception code ConditionalCheckFailed on update future action, letting flow continue actionId=" + action.getActionId())
                )
        );
        Assertions.assertTrue(
                logsList.stream().anyMatch(
                        log -> log.getLevel() == Level.INFO
                )
        );
    }


}