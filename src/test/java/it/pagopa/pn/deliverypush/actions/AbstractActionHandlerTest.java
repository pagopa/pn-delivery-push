package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.DigitalAddressSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = PnDeliveryPushConfigs.class)
@TestPropertySource(properties = {"pn.delivery-push.time-params.second-attempt-waiting-time=1s",
        "pn.delivery-push.time-params.processing-time-to-recipient=1s",
        "pn.delivery-push.time-params.waiting-response-from-first-address=1s",
        "pn.delivery-push.time-params.waiting-for-next-action=1s",
        "pn.delivery-push.time-params.time-between-ext-ch-reception-and-message-processed=1s",
        "pn.delivery-push.time-params.interval-between-notification-and-message-received=1s"})
public class AbstractActionHandlerTest {

    private Action action;
    private Action.ActionBuilder actionBuilder;

    @Mock
    private TimelineDao timelineDao;

    @Mock
    private ActionsPool actionsPool;

    @Autowired
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private TestActionHandler testActionHandler;

    private Instant now;

    @BeforeEach
    public void setup(){
        now = Instant.now();

        action = Action.builder()
                .notBefore( now.plus(pnDeliveryPushConfigs.getTimeParams().getSecondAttemptWaitingTime()) )
                .type( ActionType.SEND_PEC )
                .digitalAddressSource( DigitalAddressSource.GENERAL )
                .retryNumber( 2 )
                .build();
        actionBuilder = action.toBuilder();
    }

    @Test
    public void testActionFirstRound() throws Exception {

        testActionHandler = new TestActionHandler(timelineDao, actionsPool, pnDeliveryPushConfigs);
        action = testActionHandler.actionInFirstRound(actionBuilder, action);

        assertEquals(action.getNotBefore().minus(Duration.ofSeconds(1)), action.getNotBefore().minus(pnDeliveryPushConfigs.getTimeParams().getSecondAttemptWaitingTime())) ;

    }

    @Test
    public void testActionSecondRound() throws Exception {

        testActionHandler = new TestActionHandler(timelineDao, actionsPool, pnDeliveryPushConfigs);
        action = testActionHandler.actionInSecondRound(actionBuilder, action);

        assertEquals(action.getNotBefore().minus(Duration.ofSeconds(1)), action.getNotBefore().minus(pnDeliveryPushConfigs.getTimeParams().getSecondAttemptWaitingTime())) ;
    }


    public static class TestActionHandler extends AbstractActionHandler {

        protected TestActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
            super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        }

        @Override
        public void handleAction(Action action, Notification notification) {

        }

        @Override
        public ActionType getActionType() {
            return null;
        }
    }

}
