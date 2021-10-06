package it.pagopa.pn.deliverypush.actions;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;

class PecFailReceivePaperFeedbackActionHandlerTest {
    private TimelineDao timelineDao;
    private ActionsPool actionsPool;
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private PecFailReceivePaperFeedbackActionHandler handler;

    @BeforeEach
    void setup() {
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        timelineDao = Mockito.mock(TimelineDao.class);
        actionsPool = Mockito.mock(ActionsPool.class);
        handler = new PecFailReceivePaperFeedbackActionHandler(
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
    						.iun( "IUN_01" )
    						.actionId( "IUN_01_send_paper_result_rec0" )
    						.type( ActionType.END_OF_DIGITAL_DELIVERY_WORKFLOW )
    						.recipientIndex( 0 )
    						.responseStatus( PnExtChnProgressStatus.OK )
    						.build();

        String actionId = action.getType().buildActionId( action );
        action = action.toBuilder().actionId( actionId ).build();

        Notification notification = newNotificationWithoutPayments();
        NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
        
        Mockito.when(timelineDao.getTimelineElement( Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( Optional.of( TimelineElement.builder()
                        .category( TimelineElementCategory.SEND_PAPER_FEEDBACK )
                        .details( new SendPaperFeedbackDetails ( SendPaperDetails.builder()
                        			.address( recipient.getPhysicalAddress() )
                        			.build(),
                                    Collections.singletonList( action.getResponseStatus().name() )
                        ))
                        .build() ) );

        //When
        handler.handleAction( action, notification );

        //Then
        Mockito.verify( timelineDao ).addTimelineElement( Mockito.any(TimelineElement.class) );
    }

    @Test
    void successGetActionType() {
        //When
        ActionType actionType = handler.getActionType();
        //Then
        Assertions.assertEquals(ActionType.PEC_FAIL_RECEIVE_PAPER, actionType, "Different Action Type");
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
