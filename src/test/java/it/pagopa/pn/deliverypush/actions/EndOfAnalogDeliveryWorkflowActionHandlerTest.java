package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;

class EndOfAnalogDeliveryWorkflowActionHandlerTest {

	private EndOfAnalogDeliveryWorkflowActionHandler handler;
	private TimelineDao timelineDao;
	private ActionsPool actionsPool;
	private PnDeliveryPushConfigs pnDeliveryPushConfigs;
	private LegalFactDao legalFactStore;
	
	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setup() {
		timelineDao = Mockito.mock( TimelineDao.class );
		actionsPool = Mockito.mock( ActionsPool.class );
		legalFactStore = Mockito.mock( LegalFactDao.class );
		pnDeliveryPushConfigs = Mockito.mock( PnDeliveryPushConfigs.class );
		TimeParams times = new TimeParams();
		times.setRecipientViewMaxTimeForAnalog( Duration.ZERO );
		Mockito.when( pnDeliveryPushConfigs.getTimeParams() ).thenReturn( times );
		handler = new EndOfAnalogDeliveryWorkflowActionHandler(
				timelineDao,
				actionsPool,
				legalFactStore,
				pnDeliveryPushConfigs );
	}
	
	@Test
    void successHandleAction() {
		
		//Given
		Action action = newAction();
	    Notification notification = newNotification();

	    
	    //When
	    handler.handleAction( action, notification );
	    
		//Then
		ArgumentCaptor<TimelineElement> timeLineArg = ArgumentCaptor.forClass(TimelineElement.class);
		Mockito.verify(timelineDao).addTimelineElement(timeLineArg.capture());
		Assertions.assertEquals( TimelineElementCategory.END_OF_ANALOG_DELIVERY_WORKFLOW , timeLineArg.getValue().getCategory());

		ArgumentCaptor<Action> actionArg = ArgumentCaptor.forClass(Action.class);
		Mockito.verify(actionsPool).scheduleFutureAction(actionArg.capture());
		Assertions.assertEquals(ActionType.WAIT_FOR_RECIPIENT_TIMEOUT , actionArg.getValue().getType());
	}

	@Test
	void successGetActionType() {
		//When
		ActionType actionType = handler.getActionType();
		//Then
		Assertions.assertEquals(ActionType.END_OF_ANALOG_DELIVERY_WORKFLOW, actionType, "Different Action Type");
	}

	private TimelineElement newTimelineElement(List<DigitalAddress> addresses) {
		return TimelineElement.builder()
				.details( NotificationPathChooseDetails.builder()
						.courtesyAddresses(addresses)
						.build()
				)
				.build();
	}

	private NotificationPathChooseDetails newNotificationPathChooseDetails() {
		List<DigitalAddress> addresses = Arrays.asList(
				DigitalAddress.builder()
					.type( DigitalAddressType.EMAIL )
					.address( "nome1.cognome1@develop1.it" )
					.build(),
				DigitalAddress.builder()
					.type( DigitalAddressType.EMAIL )
					.address( "nome2.cognome2@develop2.it" )
					.build()
			);
		
		return NotificationPathChooseDetails.builder()
				.taxId( "CGNNMO80A01H501M" )
				.courtesyAddresses( addresses )
				.build();
	}
	
	private Action newAction() {
		return Action.builder()
				.iun( "IUN_01" )
				.actionId( "IUN_01_end_analog_rec0" )
				.type( ActionType.END_OF_ANALOG_DELIVERY_WORKFLOW)
				.recipientIndex( 0 )
				.build();
	}
	
	private Notification newNotification() {			
		return Notification.builder()
		        .iun( "IUN_01" )
		        .cancelledIun( "string" )
		        .paNotificationId( "proto01" )
		        .subject( "Local Subject" )
		        .sender(NotificationSender.builder()
		                .paId(" pa_02")
		                .build() 
		                )
		        .recipients( Collections.singletonList(
		                NotificationRecipient.builder()
		                        .taxId( "CGNNMO80A01H501M" )
		                        .denomination( "Nome1 Cognome1" )
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
		                        )
		        )
		        .build();
	}
}
