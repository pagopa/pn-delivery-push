package it.pagopa.pn.deliverypush;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementDetails;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.DigitalAddressSource;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.actions.AbstractActionHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

class AbstractActionHandlerTest {
    private TimelineDao timelineDao;
    private ActionsPool actionsPool;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private TestAbstractActionHandler abstractActionHandler;

    @BeforeEach
    void setup() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        timelineDao = Mockito.mock(TimelineDao.class);
        actionsPool = Mockito.mock(ActionsPool.class);
        /*abstractActionHandler = Mockito.mock(AbstractActionHandler.class, Mockito.withSettings()
                .useConstructor(timelineDao, actionsPool)
                .defaultAnswer(Mockito.CALLS_REAL_METHODS)
        );*/
        abstractActionHandler = new TestAbstractActionHandler(timelineDao, actionsPool, pnDeliveryPushConfigs);
        TimeParams times = new TimeParams();
        times.setRecipientViewMaxTime(Duration.ZERO);
        times.setSecondAttemptWaitingTime(Duration.ZERO);
        times.setIntervalBetweenNotificationAndMessageReceived(Duration.ZERO);
        times.setWaitingForNextAction(Duration.ZERO);
        times.setTimeBetweenExtChReceptionAndMessageProcessed(Duration.ZERO);
        times.setWaitingResponseFromFirstAddress(Duration.ZERO);
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
    }

    @Test
    void successScheduleAction() {
        //Given
        Action.ActionBuilder nextActionBuilder = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(1);
        Action action = nextActionBuilder
                .actionId(ActionType.SEND_PEC.buildActionId(Action.builder()
                        .iun("Test_iun01")
                        .recipientIndex(1)
                        .build()))
                .type(ActionType.SEND_PEC)
                .build();

        //When
        abstractActionHandler.scheduleAction(action);

        //Then
        ArgumentCaptor<Action> actionCapture = ArgumentCaptor.forClass(Action.class);
        Mockito.verify(actionsPool).scheduleFutureAction(actionCapture.capture());

        //Assertions.assertEquals(action, actionCapture.getValue(), "Different action from expected");
        //Assertions.assertSame(action,actionCapture.getValue(),"Not equal");
        Assertions.assertTrue(new ReflectionEquals(action, "").matches(actionCapture.getValue()));
    }

    @Test
    void successAddTimelineElementTest() {
        //Given
        Action.ActionBuilder nextActionBuilder = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(1);
        Action action = nextActionBuilder
                .actionId(ActionType.SEND_PEC.buildActionId(Action.builder()
                        .iun("Test_iun01")
                        .recipientIndex(1)
                        .build()))
                .type(ActionType.SEND_PEC)
                .build();

        TimelineElement row = TimelineElement.builder()
                .iun("Test_iun01")
                .timestamp(Instant.now())
                .elementId(action.getActionId())
                .build();
        //When
        abstractActionHandler.addTimelineElement(action, row);

        //Then
        ArgumentCaptor<TimelineElement> timelineElementCapture = ArgumentCaptor.forClass(TimelineElement.class);
        Mockito.verify(timelineDao).addTimelineElement(timelineElementCapture.capture());

        Assertions.assertTrue(new ReflectionEquals(row, "timestamp").matches(timelineElementCapture.getValue()));
    }

    @Test
    void successGetTimelineElement() {
        //Given
        Action.ActionBuilder nextActionBuilder = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(1);
        Action action = nextActionBuilder
                .actionId(ActionType.SEND_PEC.buildActionId(Action.builder()
                        .iun("Test_iun01")
                        .recipientIndex(1)
                        .build()))
                .type(ActionType.SEND_PEC)
                .build();

        ActionType actionType = ActionType.CHOOSE_DELIVERY_MODE;
        Class<TimelineElementDetails> timelineDetailsClass = null;
        //NotificationPathChooseDetails notificationPathChooseDetails = Mockito.any(NotificationPathChooseDetails.class);

        //When
        abstractActionHandler.getTimelineElement(action, actionType, timelineDetailsClass);

        //Then
        ArgumentCaptor<String> actionIun = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> actionId = ArgumentCaptor.forClass(String.class);

        Mockito.verify(timelineDao).getTimelineElement(actionIun.capture(), actionId.capture());

        Assertions.assertEquals(action.getIun(), actionIun.getValue(), "Different action Iun");
        //Assertions.assertEquals(action.getActionId(), actionId.getValue(), "Different action Id");
    }

    @Test
    void successBuildNextSendActionFirstTest() {
        //Given
        Action.ActionBuilder nextActionBuilder = Action.builder()
                .iun("Test_iun01")
                .retryNumber(1)
                .digitalAddressSource(DigitalAddressSource.PLATFORM)
                .recipientIndex(1);
        Action action1 = nextActionBuilder
                .actionId(ActionType.SEND_PEC.buildActionId(Action.builder()
                        .iun("Test_iun01")
                        .recipientIndex(1)
                        .retryNumber(1)
                        .build()))
                .type(ActionType.SEND_PEC)
                .build();

        //When
        Optional<Action> nextAction = abstractActionHandler.buildNextSendAction(action1);

        //Then
        //ArgumentCaptor<Action> actionCapture1 = ArgumentCaptor.forClass(Action.class);

        //Optional<Action> nextAction = Mockito.verify(abstractActionHandler).buildNextSendAction(action1);

        Assertions.assertEquals(action1.getIun(), nextAction.get().getIun(), "Different Iun");
        //Assertions.assertEquals(action1.getRecipientIndex(), actionCapture1.getValue().getRecipientIndex(), "Different recipient index");
        Assertions.assertEquals(1, nextAction.get().getRetryNumber());

    }

    @Test
    void successBuildNextSendActionSecondTest() {
        //Given
        Action.ActionBuilder nextActionBuilder2 = Action.builder()
                .iun("Test_iun01")
                .retryNumber(2)
                .digitalAddressSource(DigitalAddressSource.PLATFORM)
                .recipientIndex(1);
        Action action2 = nextActionBuilder2
                .actionId(ActionType.SEND_PEC.buildActionId(Action.builder()
                        .iun("Test_iun01")
                        .recipientIndex(1)
                        .retryNumber(2)
                        .build()))
                .type(ActionType.SEND_PEC)
                .build();

        //When
        Optional<Action> nextAction = abstractActionHandler.buildNextSendAction(action2);

        //Then
        //ArgumentCaptor<Action> actionCapture2 = ArgumentCaptor.forClass(Action.class);
        //Optional<Action> nextAction = Mockito.verify(abstractActionHandler).buildNextSendAction(actionCapture2.capture());

        Assertions.assertEquals(action2.getIun(), nextAction.get().getIun(), "Different Iun");
        //Assertions.assertEquals(action2.getRecipientIndex(), actionCapture2.getValue().getRecipientIndex(), "Different recipient index");
        Assertions.assertEquals(2, nextAction.get().getRetryNumber(), "Different retry number");

    }

    @Test
    void successBuildNextSendActionNullTest() {
        //Given
        Action.ActionBuilder nextActionBuilder2 = Action.builder()
                .iun("Test_iun01")
                .retryNumber(3)
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .recipientIndex(1);
        Action action2 = nextActionBuilder2
                .actionId(ActionType.SEND_PEC.buildActionId(Action.builder()
                        .iun("Test_iun01")
                        .recipientIndex(1)
                        .retryNumber(2)
                        .build()))
                .type(ActionType.SEND_PEC)
                .build();

        //When
        Optional<Action> nextAction = abstractActionHandler.buildNextSendAction(action2);

        //Then
        //ArgumentCaptor<Action> actionCapture2 = ArgumentCaptor.forClass(Action.class);
        //Optional<Action> nextAction = Mockito.verify(abstractActionHandler).buildNextSendAction(actionCapture2.capture());

        //Assertions.assertEquals(action2.getIun(), nextAction.get().getIun(), "Different Iun");
        //Assertions.assertEquals(action2.getRecipientIndex(), actionCapture2.getValue().getRecipientIndex(), "Different recipient index");
        Assertions.assertEquals(Optional.empty(), nextAction);

    }

    @Test
    void successBuildWRTActionTest() {
        //Given
        Action.ActionBuilder nextActionBuilder = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(1);
        Action action = nextActionBuilder
                .actionId(ActionType.SEND_PEC.buildActionId(Action.builder()
                        .iun("Test_iun01")
                        .recipientIndex(1)
                        .build()))
                .type(ActionType.SEND_PEC)
                .build();

        //When
        Action returnAction = abstractActionHandler.buildWaitRecipientTimeoutAction(action);

        //Then
        ArgumentCaptor<Action> actionCapture = ArgumentCaptor.forClass(Action.class);

        Assertions.assertEquals(action.getIun(), returnAction.getIun(), "Different Iun");
        Assertions.assertEquals(action.getRecipientIndex(), returnAction.getRecipientIndex(), "Different action recipient index");
        Assertions.assertEquals(ActionType.WAIT_FOR_RECIPIENT_TIMEOUT, returnAction.getType());

    }

    @Test
    void successLoadFirstAttemptTime() {
        //Given
        Action.ActionBuilder nextActionBuilder = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(1);
        Action action = nextActionBuilder
                .actionId(ActionType.RECEIVE_PEC.buildActionId(Action.builder()
                        .retryNumber(1)
                        .build()))
                .type(ActionType.RECEIVE_PEC)
                .build();

        //When
        Instant res = abstractActionHandler.loadFirstAttemptTime(action);

        //Then
        ArgumentCaptor<String> actionIunCapture = ArgumentCaptor.forClass(String.class);
        Mockito.verify(timelineDao).getTimeline(actionIunCapture.capture());

        Assertions.assertEquals(action.getIun(), actionIunCapture.getValue(), "Different action Iun");
        //Assertions.assertNotEquals(Instant.now() ,timelineElement.map(TimelineElement::getTimestamp), "First attempt absent");
    }

    private static class TestAbstractActionHandler extends AbstractActionHandler {

        public TestAbstractActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
            super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        }

        @Override
        public void handleAction(Action action, Notification notification) {

        }

        @Override
        public void scheduleAction(Action action) {
            super.scheduleAction(action);
        }

        @Override
        public void addTimelineElement(Action action, TimelineElement row) {
            super.addTimelineElement(action, row);
        }

        @Override
        public <T> Optional<T> getTimelineElement(Action action, ActionType actionType, Class<T> timelineDetailsClass) {
            return super.getTimelineElement(action, actionType, timelineDetailsClass);
        }

        @Override
        public Optional<Action> buildNextSendAction(Action action) {
            return super.buildNextSendAction(action);
        }

        @Override
        public Action buildWaitRecipientTimeoutAction(Action action) {
            return super.buildWaitRecipientTimeoutAction(action);
        }

        @Override
        public Instant loadFirstAttemptTime(Action action) {
            return super.loadFirstAttemptTime(action);
        }

        @Override
        public ActionType getActionType() {
            return null;
        }
    }
}
