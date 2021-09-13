package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.DigitalAddressSource;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.actions.LegalFactUtils;
import it.pagopa.pn.deliverypush.actions.SenderAckActionHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

class SenderAckActionHandlerTest {
    private LegalFactUtils legalFactUtils;
    private TimelineDao timelineDao;
    private ActionsPool actionsPool;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private SenderAckActionHandler handler;

    @BeforeEach
    public void setup() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        legalFactUtils = Mockito.mock(LegalFactUtils.class);
        timelineDao = Mockito.mock(TimelineDao.class);
        actionsPool = Mockito.mock(ActionsPool.class);
        handler = new SenderAckActionHandler(
                legalFactUtils,
                timelineDao,
                actionsPool,
                pnDeliveryPushConfigs
        );
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
    void successHandleAction() {
        //Given
        Notification notification = newNotificationWithoutPayments();

        Action action = Action.builder()
                .iun( notification.getIun() )
                .recipientIndex(0)
                .type(ActionType.SENDER_ACK)
                .retryNumber(1)
                .notBefore(Instant.now())
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .actionId("Test_iun01_send_pec_rec0_null_nnull")
                .build();

        String actionId = action.getType().buildActionId(action);
        action = action.toBuilder().actionId(actionId).build();


        //When
        handler.handleAction(action, notification);

        //Then
        ArgumentCaptor<Action> actionCapture = ArgumentCaptor.forClass(Action.class);
        ArgumentCaptor<Notification> notificationCapture = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<NotificationRecipient> notificationRecipientCapture = ArgumentCaptor.forClass(NotificationRecipient.class);

        Mockito.verify(legalFactUtils).saveNotificationReceivedLegalFact( actionCapture.capture(),
                            notificationCapture.capture(), notificationRecipientCapture.capture() );

        Assertions.assertEquals(action.getIun(), actionCapture.getValue().getIun(), "Different iun");
        Assertions.assertEquals( action.getIun(), notificationCapture.getValue().getIun(), "Different iun");
        Assertions.assertEquals( notification.getRecipients().get(0).getTaxId(), notificationRecipientCapture.getValue().getTaxId() );
    }


    @Test
    void getActionTypeTest() {
        //When
        ActionType actionType = handler.getActionType();
        //Then
        Assertions.assertEquals(ActionType.SENDER_ACK, actionType, "Different Action Type");
    }

    private Notification newNotificationWithoutPayments() {
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
                .documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .savedVersionId("v01_doc00")
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .contentType("application/pdf")
                                .body("Ym9keV8wMQ==")
                                .build(),
                        NotificationAttachment.builder()
                                .savedVersionId("v01_doc01")
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .contentType("application/pdf")
                                .body("Ym9keV8wMg==")
                                .build()
                ))
                .build();
    }
}
