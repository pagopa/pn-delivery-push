package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons.abstractions.MomProducer;
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
import java.util.Collections;
import java.util.Optional;

class ReceivePecActionHandlerTest {

    private TimelineDao timelineDao;
    private ActionsPool actionsPool;
    private ReceivePecActionHandler handler;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private ExtChnEventUtils extChnEventUtils;

    @BeforeEach
    public void setup() {
        timelineDao = Mockito.mock(TimelineDao.class);
        actionsPool = Mockito.mock(ActionsPool.class);
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        paperRequestProducer = Mockito.mock(MomProducer.class);
        extChnEventUtils = Mockito.mock(ExtChnEventUtils.class);
        handler = new ReceivePecActionHandler(
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
    void successHandleActionTest() {
        //Given
        Action action = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(0)
                .type(ActionType.CHOOSE_DELIVERY_MODE)
                .retryNumber(1)
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .responseStatus(PnExtChnProgressStatus.PERMANENT_FAIL)
                .actionId("Test_iun01_send_pec_rec0_null_nnull")
                .build();

        String actionId = action.getType().buildActionId(action);
        action = action.toBuilder().actionId(actionId).build();

        Mockito.when(timelineDao.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(TimelineElement.builder()
                        .details(NotificationPathChooseDetails.builder()
                                .general(DigitalAddress.builder()
                                        .address("account@dominio.it")
                                        .type(DigitalAddressType.PEC)
                                        .build())
                                .build())
                        .build()));

        Notification notification = newNotificationWithoutPayments();

        //When
        handler.handleAction(action, notification);

        //Then
        Mockito.verify(timelineDao).addTimelineElement(Mockito.any(TimelineElement.class));
    }

    @Test
    void getActionTypeTest() {
        //When
        ActionType actionType = handler.getActionType();
        //Then
        Assertions.assertEquals(ActionType.RECEIVE_PEC, actionType, "Different Action Type");
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
