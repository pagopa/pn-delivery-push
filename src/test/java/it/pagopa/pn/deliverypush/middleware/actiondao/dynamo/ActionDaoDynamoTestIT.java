package it.pagopa.pn.deliverypush.middleware.actiondao.dynamo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.ActionDaoDynamo;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        ActionDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class ActionDaoDynamoTestIT {
    @Autowired
    private ActionDao actionDao;
    
    @Test
    void addAndCheckAction() {
        //GIVEN
        
        String timeSlot = "2022-04-12T09:26";

        Action.ActionBuilder actionBuilder = Action.builder()
                .iun("Test_addAndCheckAction_iun01")
                .recipientIndex(1)
                .type(ActionType.ANALOG_WORKFLOW);
        String actionId = ActionType.ANALOG_WORKFLOW.buildActionId(
                actionBuilder.build());
        
        Action action = actionBuilder.actionId(actionId).build();
        
        Action.ActionBuilder actionBuilder2 = Action.builder()
                .iun("Test_addAndCheckAction_iun02")
                .recipientIndex(0)
                .type(ActionType.REFINEMENT_NOTIFICATION)
                .notBefore(Instant.now());
        
        String actionId2 =  ActionType.REFINEMENT_NOTIFICATION.buildActionId(
                actionBuilder2.build()
        );

        Action action2 = actionBuilder2.actionId(actionId2).build();
        
        //WHEN
        actionDao.addAction(action, timeSlot);
        actionDao.addAction(action2, timeSlot);
        
        //THEN
        Optional<Action> actionOpt =  actionDao.getActionById(actionId);
        Assertions.assertTrue(actionOpt.isPresent());
        Assertions.assertEquals(actionOpt.get(),action);
        
        Optional<Action> actionOpt2 =  actionDao.getActionById(actionId2);
        Assertions.assertTrue(actionOpt2.isPresent());
        Assertions.assertEquals(actionOpt2.get(),action2);

        actionDao.unSchedule(action, timeSlot);
        actionDao.unSchedule(action2, timeSlot);
    }

    
    
    @Test
    void addAndCheckStartRecipientWorkFlowAction() {
        //GIVEN
        
        String timeSlot = "2022-04-12T09:26";

        Action.ActionBuilder actionBuilder = Action.builder()
                .iun("Test_addAndCheckStartRecipientWorkFlowAction_iun01")
                .recipientIndex(1)
                .type(ActionType.START_RECIPIENT_WORKFLOW);
        String actionId = ActionType.START_RECIPIENT_WORKFLOW.buildActionId(
                actionBuilder.build());
        
        Action action = actionBuilder.actionId(actionId).details(new RecipientsWorkflowDetails("token")).build();
           
        //WHEN
        actionDao.addAction(action, timeSlot);
        
        //THEN
        Optional<Action> actionOpt =  actionDao.getActionById(actionId);
        Assertions.assertTrue(actionOpt.isPresent());
        Assertions.assertEquals(actionOpt.get(),action);
     

        actionDao.unSchedule(action, timeSlot);
    }

    
    
    
    
    @Test
    void addAndCheckFutureActionSameTimeSlot() {
        //GIVEN
        String timeSlot = "2022-04-12T09:26";

        Action.ActionBuilder actionBuilder = Action.builder()
                .iun("Test_addAndCheckFutureActionSameTimeSlot_iun01")
                .recipientIndex(1)
                .type(ActionType.DIGITAL_WORKFLOW_NEXT_ACTION);
        String actionId = ActionType.DIGITAL_WORKFLOW_NEXT_ACTION.buildActionId(
                actionBuilder.build());

        Action action = actionBuilder.actionId(actionId).build();

        Action.ActionBuilder actionBuilder2 = Action.builder()
                .iun("Test_addAndCheckFutureActionSameTimeSlot_iun02")
                .recipientIndex(0)
                .type(ActionType.ANALOG_WORKFLOW)
                .notBefore(Instant.now());

        String actionId2 =  ActionType.ANALOG_WORKFLOW.buildActionId(
                actionBuilder2.build()
        );

        Action action2 = actionBuilder2.actionId(actionId2).build();

        actionDao.unSchedule(action, timeSlot);
        actionDao.unSchedule(action2, timeSlot);

        //WHEN
        actionDao.addAction(action, timeSlot);
        actionDao.addAction(action2, timeSlot);

        //THEN
        List<Action> actions =  actionDao.findActionsByTimeSlot( timeSlot );
        Assertions.assertEquals(2, actions.size());
        Assertions.assertTrue(actions.contains(action));
        Assertions.assertTrue(actions.contains(action2));

        actionDao.unSchedule(action, timeSlot);
        actionDao.unSchedule(action2, timeSlot);

    }

    @Test
    void addAndCheckFutureActionDifferentTimeSlot() {
        //GIVEN

        Action.ActionBuilder actionBuilder = Action.builder()
                .iun("Test_addAndCheckFutureActionDifferentTimeSlot_iun01")
                .recipientIndex(1)
                .type(ActionType.DIGITAL_WORKFLOW_NEXT_ACTION);
        String actionId = ActionType.DIGITAL_WORKFLOW_NEXT_ACTION.buildActionId(
                actionBuilder.build());

        Action action = actionBuilder.actionId(actionId).build();

        Action.ActionBuilder actionBuilder2 = Action.builder()
                .iun("Test_addAndCheckFutureActionDifferentTimeSlot_iun02")
                .recipientIndex(0)
                .type(ActionType.ANALOG_WORKFLOW)
                .notBefore(Instant.now());

        String actionId2 =  ActionType.ANALOG_WORKFLOW.buildActionId(
                actionBuilder2.build()
        );

        Action action2 = actionBuilder2.actionId(actionId2).build();

        String timeSlot1 = "2022-04-12T09:26";
        String timeSlot2 = "2022-04-12T09:27";
        
        List<Action> actionslist1 =  actionDao.findActionsByTimeSlot( timeSlot1 );

        actionDao.unSchedule(action, timeSlot1);
        actionDao.unSchedule(action2, timeSlot1);
        actionDao.unSchedule(action2, timeSlot2);

        List<Action> actionslist2 =  actionDao.findActionsByTimeSlot( timeSlot1 );

        //WHEN
        actionDao.addAction(action, timeSlot1);
        actionDao.addAction(action2, timeSlot2);

        //THEN
        List<Action> actions =  actionDao.findActionsByTimeSlot( timeSlot1 );
        Assertions.assertEquals(1, actions.size());
        Assertions.assertTrue(actions.contains(action));
        Assertions.assertFalse(actions.contains(action2));

        List<Action> actions2 =  actionDao.findActionsByTimeSlot( timeSlot2 );
        Assertions.assertEquals(1, actions2.size());
        Assertions.assertTrue(actions2.contains(action2));
        Assertions.assertFalse(actions2.contains(action));

        actionDao.unSchedule(action, timeSlot1);
        actionDao.unSchedule(action2, timeSlot2);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void addActionIfAbsent() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";

        Action.ActionBuilder actionBuilder = Action.builder()
                .iun("Test_addActionIfAbsent_iun01")
                .recipientIndex(2)
                .type(ActionType.DIGITAL_WORKFLOW_NEXT_ACTION);
        String actionId = ActionType.DIGITAL_WORKFLOW_NEXT_ACTION.buildActionId(
                actionBuilder.build());

        Action action = actionBuilder.actionId(actionId).build();


        // non si riesce a mockare TransactWriteItemsEnhancedRequest
        Assertions.assertDoesNotThrow(() ->
                actionDao.addActionIfAbsent(action, timeslot)
        );
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void addActionIfAbsentFailSilent() {
        String timeslot = "2022-08-30T16:04:13.913859900Z";

        Action.ActionBuilder actionBuilder = Action.builder()
                .iun("Test_addIfAbsentFailSilent_iun01")
                .recipientIndex(1)
                .type(ActionType.DIGITAL_WORKFLOW_RETRY_ACTION);
        String actionId = ActionType.DIGITAL_WORKFLOW_NEXT_ACTION.buildActionId(
                actionBuilder.build());

        Action action = actionBuilder.actionId(actionId).build();


        // get Logback Logger
        Logger fooLogger = (Logger) LoggerFactory.getLogger(ActionDaoDynamo.class);

        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        // addAppender is outdated now
        ((ch.qos.logback.classic.Logger)fooLogger).addAppender(listAppender);


        // non si riesce a mockare TransactWriteItemsEnhancedRequest
        Assertions.assertDoesNotThrow(() ->
                actionDao.addActionIfAbsent(action, timeslot)
        );

        Assertions.assertDoesNotThrow(() ->
                actionDao.addActionIfAbsent(action, timeslot)
        );

        // JUnit assertions
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Exception code ConditionalCheckFailed is expected for retry, letting flow continue actionId=Test_addIfAbsentFailSilent_iun01_digital_workflow_e_1_timelineid_ cancellationReason is CancellationReason(Code=ConditionalCheckFailed, Message=The conditional request failed)", logsList.get(0)
                .getFormattedMessage());
        assertEquals(Level.WARN, logsList.get(0)
                .getLevel());
    }
}