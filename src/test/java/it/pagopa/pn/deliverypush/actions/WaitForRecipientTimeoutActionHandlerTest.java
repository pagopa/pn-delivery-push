package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

class WaitForRecipientTimeoutActionHandlerTest {
    private TimelineDao timelineDao;
    private ActionsPool actionsPool;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private WaitForRecipientTimeoutActionHandler handler;

    @BeforeEach
    void setup() {
        timelineDao = Mockito.mock(TimelineDao.class);
        actionsPool = Mockito.mock(ActionsPool.class);
        pnDeliveryPushConfigs = Mockito.mock((PnDeliveryPushConfigs.class));
        handler = new WaitForRecipientTimeoutActionHandler(
                timelineDao,
                actionsPool,
                pnDeliveryPushConfigs
        );
        TimeParams times = new TimeParams();
        times.setRecipientViewMaxTimeForDigital(Duration.ZERO);
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
        Action action = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(0)
                .type(ActionType.SEND_PEC)
                .retryNumber(1)
                .notBefore(Instant.now())
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .actionId("Test_iun01_send_pec_rec0_null_nnull")
                .build();

        String actionId = action.getType().buildActionId(action);
        action = action.toBuilder().actionId(actionId).build();

        Notification notification = newNotificationWithoutPayments();

        //When
        handler.handleAction(action, notification);

        //Then
        Mockito.verify(timelineDao).addTimelineElement(Mockito.any(TimelineElement.class));

    }

    @Test
    void successGetActionType() {
        //When
        ActionType actionType = handler.getActionType();
        //Then
        Assertions.assertEquals(ActionType.WAIT_FOR_RECIPIENT_TIMEOUT, actionType, "Different Action Type");
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
                .build();
    }
}
