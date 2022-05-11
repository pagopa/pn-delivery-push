package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.middleware.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;

class NotificationViewedActionHandlerTest {
    private LegalFactDao legalFactDao;
    private TimelineDao timelineDao;
    private ActionsPool actionsPool;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private NotificationViewedActionHandler handler;
    private PaperNotificationFailedDao paperNotificationFailedDao;
    private Instant instant;


    private static class TestInstantSupplier extends InstantNowSupplier {
        private final Instant value;

        TestInstantSupplier( Instant instant) {
            this.value = instant;
        }

        @Override
        public Instant get() {
            return this.value;
        }
    }

    @BeforeEach
    public void setup() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        legalFactDao = Mockito.mock(LegalFactDao.class);
        timelineDao = Mockito.mock(TimelineDao.class);
        actionsPool = Mockito.mock(ActionsPool.class);
        paperNotificationFailedDao = Mockito.mock(PaperNotificationFailedDao.class);

        this.instant = Instant.now();

        handler = new NotificationViewedActionHandler(
                timelineDao,
                actionsPool,
                legalFactDao,
                pnDeliveryPushConfigs,
                paperNotificationFailedDao,
                new TestInstantSupplier( this.instant )
            );
    }

    @Test
    void successHandleAction() {
        //Given
        Notification notification = newNotification();

        Action action = Action.builder()
                .iun(notification.getIun())
                .recipientIndex(0)
                .type(ActionType.NOTIFICATION_VIEWED)
                .actionId("Test_iun01_notification_viewed_0")
                .build();

        String actionId = action.getType().buildActionId(action);
        action = action.toBuilder().actionId(actionId).build();

        //When
        handler.handleAction(action, notification);

        //Then
        Mockito.verify(legalFactDao).saveNotificationViewedLegalFact( notification,
                                   notification.getRecipients().get(0), this.instant );
        Mockito.verify(timelineDao).addTimelineElement(Mockito.any(TimelineElement.class));
    }

    @Test
    void getActionTypeTest() {
        //When
        ActionType actionType = handler.getActionType();
        //Then
        Assertions.assertEquals(ActionType.NOTIFICATION_VIEWED, actionType, "Different Action Type");
    }

    private Notification newNotification() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
}
