package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.deliverypush.middleware.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

class NotificationViewedActionHandlerTest {
    private LegalFactUtils legalFactUtils;
    private TimelineDao timelineDao;
    private ActionsPool actionsPool;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private NotificationViewedActionHandler handler;
    private PaperNotificationFailedDao paperNotificationFailedDao;

    @BeforeEach
    public void setup() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        legalFactUtils = Mockito.mock(LegalFactUtils.class);
        timelineDao = Mockito.mock(TimelineDao.class);
        actionsPool = Mockito.mock(ActionsPool.class);
        paperNotificationFailedDao = Mockito.mock(PaperNotificationFailedDao.class);
        handler = new NotificationViewedActionHandler(
                timelineDao,
                actionsPool,
                legalFactUtils,
                pnDeliveryPushConfigs,
                paperNotificationFailedDao
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
        Mockito.verify(legalFactUtils).saveNotificationViewedLegalFact(action, notification);
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
