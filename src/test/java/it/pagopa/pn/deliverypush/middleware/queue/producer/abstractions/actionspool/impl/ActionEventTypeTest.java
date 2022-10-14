package it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ActionEventTypeTest {


    @Test
    void getEventJavaClass() {
        ActionEventType actionGeneric = ActionEventType.ACTION_GENERIC;
        Class<?> action = actionGeneric.getEventJavaClass();
        Assertions.assertNotNull(action);
    }
}