package it.pagopa.pn.deliverypush.actions;
/*
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;

import SendPaperDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

class ReceivePaperActionHandlerTest {

    private MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private TimelineDao timelineDao;
    private ActionsPool actionsPool;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private ExtChnEventUtils extChnEventUtils;
    private ReceivePaperActionHandler handler;

    @BeforeEach
    void setup() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        paperRequestProducer = Mockito.mock(MomProducer.class);
        timelineDao = Mockito.mock(TimelineDao.class);
        actionsPool = Mockito.mock(ActionsPool.class);
        extChnEventUtils = Mockito.mock(ExtChnEventUtils.class);
        handler = new ReceivePaperActionHandler(
                timelineDao,
                actionsPool,
                pnDeliveryPushConfigs
        );
        TimeParams times = new TimeParams();
        times.setRecipientViewMaxTimeForAnalog(Duration.ZERO);
        times.setSecondAttemptWaitingTime(Duration.ZERO);
        times.setIntervalBetweenNotificationAndMessageReceived(Duration.ZERO);
        times.setWaitingForNextAction(Duration.ZERO);
        times.setTimeBetweenExtChReceptionAndMessageProcessed(Duration.ZERO);
        times.setWaitingResponseFromFirstAddress(Duration.ZERO);
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
    }

    @Test
    void successHandleActionFirstAttempt() {

        //Given
        // Action primo tentativo
        Action inputAction = Action.builder()
                .type(ActionType.RECEIVE_PAPER)
                .responseStatus(PnExtChnProgressStatus.OK)
                .iun("test_iun")
                .retryNumber(1)
                .notBefore(Instant.now())
                .recipientIndex(0)
                .actionId("test_iun_send_paper_result_rec0_n1")
                .build();

        Notification notification = newNotificationWithoutPayments();

        Mockito.when(timelineDao.getTimelineElement(
                        Mockito.anyString(),
                        Mockito.anyString()))
                .thenReturn(Optional.of(TimelineElementInternal.timelineInternalBuilder()
                        .details(SendPaperDetails.builder()
                                .address(PhysicalAddress.builder()
                                        .at("presso")
                                        .address("via di casa sua")
                                        .addressDetails("scala A")
                                        .zip("00100")
                                        .municipality("Roma")
                                        .province("RM")
                                        .foreignState("IT")
                                        .build())
                                .build())
                        .build()));


        //When
        handler.handleAction(inputAction, notification);

        //Then
        ArgumentCaptor<TimelineElementInternal> timeLineArg = ArgumentCaptor.forClass(TimelineElement.class);
        Mockito.verify(timelineDao).addTimelineElement(timeLineArg.capture());
        Assertions.assertEquals(TimelineElementCategory.SEND_PAPER_FEEDBACK, timeLineArg.getValue().getCategory());

        ArgumentCaptor<Action> actionArg = ArgumentCaptor.forClass(Action.class);
        Mockito.verify(actionsPool).scheduleFutureAction(actionArg.capture());
        Assertions.assertEquals(ActionType.END_OF_ANALOG_DELIVERY_WORKFLOW, actionArg.getValue().getType());
    }

    @Test
    void successHandleActionSecondAttempt() {

        //Given
        // Action secondo tentativo
        Action inputAction = Action.builder()
                .type(ActionType.RECEIVE_PAPER)
                .responseStatus(PnExtChnProgressStatus.RETRYABLE_FAIL)
                .iun("test_iun")
                .retryNumber(2)
                .newPhysicalAddress(PhysicalAddress.builder()
                        .at("presso altro")
                        .address("altra via di casa sua")
                        .addressDetails("scala B")
                        .zip("00100")
                        .municipality("Roma")
                        .province("RM")
                        .foreignState("IT")
                        .build())
                .notBefore(Instant.now())
                .recipientIndex(0)
                .actionId("test_iun_send_paper_result_rec0_n2")
                .build();

        Notification notification = newNotificationWithoutPayments();

        Mockito.when(timelineDao.getTimelineElement(
                        Mockito.anyString(),
                        Mockito.anyString()))
                .thenReturn(Optional.of(TimelineElementInternal.timelineInternalBuilder()
                        .details(SendPaperDetails.builder()
                                .address(PhysicalAddress.builder()
                                        .at("presso")
                                        .address("via di casa sua")
                                        .addressDetails("scala A")
                                        .zip("00100")
                                        .municipality("Roma")
                                        .province("RM")
                                        .foreignState("IT")
                                        .build())
                                .build())
                        .build()));


        //When
        handler.handleAction(inputAction, notification);

        //Then
        ArgumentCaptor<TimelineElementInternal> timeLineArg = ArgumentCaptor.forClass(TimelineElement.class);
        Mockito.verify(timelineDao).addTimelineElement(timeLineArg.capture());
        Assertions.assertEquals(TimelineElementCategory.SEND_PAPER_FEEDBACK, timeLineArg.getValue().getCategory());

        ArgumentCaptor<Action> actionArg = ArgumentCaptor.forClass(Action.class);
        Mockito.verify(actionsPool).scheduleFutureAction(actionArg.capture());
        Assertions.assertEquals(ActionType.SEND_PAPER, actionArg.getValue().getType());
    }

    @Test
    void successGetActionType() {
        //When
        ActionType actionType = handler.getActionType();
        //Then
        Assertions.assertEquals(ActionType.RECEIVE_PAPER, actionType, "Different Action Type");
    }

    private Notification newNotificationWithoutPayments() {
        return Notification.builder()
                .iun("test_iun")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(PhysicalAddress.builder()
                                        .at("presso")
                                        .address("via di casa sua")
                                        .addressDetails("scala A")
                                        .zip("00100")
                                        .municipality("Roma")
                                        .province("RM")
                                        .foreignState("IT")
                                        .build())
                                .build()
                ))
                .build();
    }
}


 */