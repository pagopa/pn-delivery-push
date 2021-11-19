package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.api.dto.notification.timeline.CompletlyUnreachableDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.middleware.failednotification.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.DigitalAddressSource;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

class CompletelyUnreachableActionHandlerTest {
    /*@InjectMocks
    private CompletelyUnreachableActionHandler handler;*/
    private CompletelyUnreachableActionHandler handler;
    @Mock
    private PaperNotificationFailedDao paperNotificationFailedDao;
    @Mock
    private TimelineDao timelineDao;
    @Mock
    ActionsPool actionsPool;
    @Mock
    PnDeliveryPushConfigs pnDeliveryPushConfigs;


    @BeforeEach
    public void setup() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        handler = new CompletelyUnreachableActionHandler(
                timelineDao,
                paperNotificationFailedDao,
                actionsPool,
                pnDeliveryPushConfigs
        );
        TimeParams times = new TimeParams();
        times.setRefinementTimeForCompletelyUnreachable(Duration.ZERO);
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleActionNotificationNotViewed() {
        Mockito.when(timelineDao.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        Action action = getNewAction();

        Notification notification = newNotificationWithoutPayments();
        handler.handleAction(action, notification);

        //Then
        Mockito.verify(timelineDao).addTimelineElement(Mockito.any(TimelineElement.class));
        Mockito.verify(paperNotificationFailedDao).addPaperNotificationFailed(Mockito.any(PaperNotificationFailed.class));

        ArgumentCaptor<Action> actionArg = ArgumentCaptor.forClass(Action.class);
        Mockito.verify(actionsPool).scheduleFutureAction(actionArg.capture());
        Assertions.assertEquals(ActionType.WAIT_FOR_RECIPIENT_TIMEOUT , actionArg.getValue().getType());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleActionNotificationViewed() {
       Mockito.when(timelineDao.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(TimelineElement.builder()
                        .details(CompletlyUnreachableDetails.builder()
                                .taxId("testIdRecipient")
                                .build())
                        .build()));

        Action action = getNewAction();

        Notification notification = newNotificationWithoutPayments();
        handler.handleAction(action, notification);

        //Then
        Mockito.verify(timelineDao).addTimelineElement(Mockito.any(TimelineElement.class));
        Mockito.verifyNoInteractions(paperNotificationFailedDao);
    }

    private Action getNewAction() {
        Action action = Action.builder()
                .iun("Test_iun01")
                .recipientIndex(0)
                .type(ActionType.CHOOSE_DELIVERY_MODE)
                .retryNumber(1)
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .responseStatus(PnExtChnProgressStatus.PERMANENT_FAIL)
                .actionId("Test_iun01_s_completely_unreachable_testIdRecipient")
                .build();
        return action;
    }

    @Test
    void getActionType() {
        ActionType actionType = handler.getActionType();
        Assertions.assertEquals(ActionType.COMPLETELY_UNREACHABLE, actionType);
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
                                .taxId("testIdRecipient")
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