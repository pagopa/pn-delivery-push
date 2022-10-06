package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ActionTypeTest {

    @Test
    void buildActionId() {

        Action action = Action.builder()
                .iun("1")
                .actionId("1")
                .recipientIndex(1)
                .build();

        Assertions.assertAll(
                () -> Assertions.assertEquals("1_start", ActionType.SENDER_ACK.buildActionId(action)),
                () -> Assertions.assertEquals("1_refinement_notification_1", ActionType.REFINEMENT_NOTIFICATION.buildActionId(action)),
                () -> Assertions.assertEquals("1_digital_workflow_e_1", ActionType.DIGITAL_WORKFLOW_NEXT_ACTION.buildActionId(action)),
                () -> Assertions.assertEquals("1_analog_workflow_e_1", ActionType.ANALOG_WORKFLOW.buildActionId(action)),
                () -> Assertions.assertEquals("1_choose_delivery_mode_1", ActionType.CHOOSE_DELIVERY_MODE.buildActionId(action)),
                () -> Assertions.assertEquals("1_start_recipient_workflow_1", ActionType.START_RECIPIENT_WORKFLOW.buildActionId(action))
        );
    }

}