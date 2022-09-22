package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhooksPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;

class SchedulerServiceImplTest {

    @Mock
    private ActionsPool actionsPool;

    @Mock
    private WebhooksPool webhooksPool;

    private SchedulerServiceImpl schedulerService;

    @BeforeEach
    void setup() {
        actionsPool = Mockito.mock(ActionsPool.class);
        webhooksPool = Mockito.mock(WebhooksPool.class);
        schedulerService = new SchedulerServiceImpl(actionsPool, webhooksPool);

    }

    @Test
    void scheduleEvent() {
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        schedulerService.scheduleEvent("01", 3, instant, ActionType.ANALOG_WORKFLOW);

        Mockito.verify(actionsPool, Mockito.times(1)).scheduleFutureAction(action);
    }

    @Test
    void scheduleWebhookEvent() {
        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");
        WebhookAction action = WebhookAction.builder()
                .iun("01")
                .paId("02")
                .timestamp(instant)
                .eventId(instant + "_" + "03")
                .oldStatus("active")
                .newStatus("inactive")
                .timelineEventCategory("test")
                .type(WebhookEventType.REGISTER_EVENT)
                .build();

        schedulerService.scheduleWebhookEvent("02", "01", "03", instant, "active", "inactive", "test");

        Mockito.verify(webhooksPool, Mockito.times(1)).scheduleFutureAction(action);
    }

    @Test
    void testScheduleWebhookEvent() {
        WebhookAction action = WebhookAction.builder()
                .streamId("01")
                .eventId("02")
                .iun("nd")
                .delay(4)
                .type(WebhookEventType.REGISTER_EVENT)
                .build();

        schedulerService.scheduleWebhookEvent("01", "02", 4, WebhookEventType.REGISTER_EVENT);

        Mockito.verify(webhooksPool, Mockito.times(1)).scheduleFutureAction(action);
    }

    private Action buildAction(ActionType type) {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        return Action.builder()
                .iun("01")
                .actionId("01_analog_workflow_e_3")
                .notBefore(instant)
                .type(type)
                .recipientIndex(3)
                .build();
    }


}