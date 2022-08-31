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
                () -> Assertions.assertEquals(ActionType.SENDER_ACK.buildActionId(action), "1_start"),
                () -> Assertions.assertEquals(ActionType.REFINEMENT_NOTIFICATION.buildActionId(action), "1_refinement_notification_1"),
                () -> Assertions.assertEquals(ActionType.DIGITAL_WORKFLOW_NEXT_ACTION.buildActionId(action), "1_digital_workflow_e_1"),
                () -> Assertions.assertEquals(ActionType.ANALOG_WORKFLOW.buildActionId(action), "1_analog_workflow_e_1"),
                () -> Assertions.assertEquals(ActionType.CHOOSE_DELIVERY_MODE.buildActionId(action), "1_choose_delivery_mode_1"),
                () -> Assertions.assertEquals(ActionType.START_RECIPIENT_WORKFLOW.buildActionId(action), "1_start_recipient_workflow_1")
        );
    }

    @Test
    void values() {
    }
}