package it.pagopa.pn.deliverypush.middleware.actiondao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.ActionDao;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        ActionDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "aws.region-code=us-east-1",
        "aws.profile-name=${PN_AWS_PROFILE_NAME:default}",
        "aws.endpoint-url=http://localhost:4566",
})
@SpringBootTest
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
    
}