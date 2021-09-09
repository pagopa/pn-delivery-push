package it.pagopa.pn.deliverypush;

import it.pagopa.pn.api.dto.legalfacts.NotificationReceivedLegalFact;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
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

public class SenderAckActionHandlerTest {
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
        Action action = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(0)
                .type(ActionType.SENDER_ACK)
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
        ArgumentCaptor<String> iunCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> nameCapture = ArgumentCaptor.forClass(String.class);

        Mockito.verify(legalFactUtils).saveLegalFact(iunCapture.capture(), nameCapture.capture(), Mockito.any(NotificationReceivedLegalFact.class));

        Assertions.assertEquals(action.getIun(), iunCapture.getValue(), "Different iun");
        Assertions.assertEquals("sender_ack_" + notification.getRecipients().get(0).getTaxId(), nameCapture.getValue());
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
