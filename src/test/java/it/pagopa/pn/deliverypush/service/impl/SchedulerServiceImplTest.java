package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.details.SendDigitalFinalStatusResponseDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
class SchedulerServiceImplTest {

    private ActionsPool actionsPool;

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
        clock = Mockito.mock(Clock.class);

        schedulerService = new SchedulerServiceImpl(actionsPool, clock, timelineUtils, featureEnabledUtils);
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

        Mockito.verify(actionsPool, Mockito.times(1)).scheduleFutureAction(any(Action.class));
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
    void testScheduleEventNotificationCancelled(){
        //GIVEN
        Action action = buildAction(ActionType.NOTIFICATION_CANCELLATION);
        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .documentCreationType(DocumentCreationTypeInt.NOTIFICATION_CANCELLED)
                .key("key")
                .timelineId("timelineId")
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(action.getIun()))
                .thenReturn(true);

        //WHEN
        schedulerService.scheduleEvent("01", 3, instant, ActionType.NOTIFICATION_CANCELLATION, "timelineId", details);

        //THEN
        Mockito.verify(actionsPool, Mockito.times(1)).startActionOrScheduleFutureAction(any());
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