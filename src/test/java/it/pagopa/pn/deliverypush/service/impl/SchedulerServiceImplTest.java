package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.details.SendDigitalFinalStatusResponseDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookEventType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhooksPool;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;
import java.time.Instant;

@ExtendWith(SpringExtension.class)
class SchedulerServiceImplTest {

    private ActionsPool actionsPool;

    private WebhooksPool webhooksPool;

    @Mock
    private Clock clock;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private FeatureEnabledUtils featureEnabledUtils;

    
    private SchedulerServiceImpl schedulerService;
    
    @BeforeEach
    void setup() {
        actionsPool = Mockito.mock(ActionsPool.class);
        webhooksPool = Mockito.mock(WebhooksPool.class);
        clock = Mockito.mock(Clock.class);

        schedulerService = new SchedulerServiceImpl(actionsPool, webhooksPool, clock, timelineUtils, featureEnabledUtils);
    }

    
    @Test
    void scheduleEventScheduleNowIfAbsent() {
        final ActionType actionType = ActionType.SEND_DIGITAL_FINAL_STATUS_RESPONSE;
        
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString()))
                .thenReturn(false);


        schedulerService.scheduleEventNowOnlyIfAbsent("01", actionType,    SendDigitalFinalStatusResponseDetails.builder()
                .lastAttemptAddressInfo(
                        DigitalAddressInfoSentAttempt.builder()
                                .relatedFeedbackTimelineId("relatedFeedback")
                                .build()
                )
                .build());

        Mockito.verify(actionsPool, Mockito.times(1)).scheduleFutureAction(Mockito.any(Action.class));
    }
    
    @Test
    void scheduleEvent() {
        Action action = buildAction(ActionType.ANALOG_WORKFLOW);
        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(action.getIun()))
                .thenReturn(false);

        schedulerService.scheduleEvent("01", 3, instant, ActionType.ANALOG_WORKFLOW);

        Mockito.verify(actionsPool, Mockito.times(1)).startActionOrScheduleFutureAction(action);
    }

    @Test
    void scheduleEventCancelled() {
        //GIVEN
        Action action = buildAction(ActionType.DIGITAL_WORKFLOW_NEXT_ACTION);
        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");
        
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(action.getIun()))
                .thenReturn(true);
        
        //WHEN
        schedulerService.scheduleEvent("01", 3, instant, ActionType.ANALOG_WORKFLOW);
        
        //THEN
        Mockito.verify(actionsPool, Mockito.never()).startActionOrScheduleFutureAction(action);
    }
    

    @Test
    void scheduleWebhookEvent() {
        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");
        Mockito.when(clock.instant()).thenReturn(instant);
        WebhookAction action = WebhookAction.builder()
                .iun("01")
                .paId("02")
                .eventId(instant + "_" + "03")
                .timelineId("03")
                .type(WebhookEventType.REGISTER_EVENT)
                .build();

        schedulerService.scheduleWebhookEvent("02", "01", "03");

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