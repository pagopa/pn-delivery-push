package it.pagopa.pn.deliverypush.actions;

import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;

class EndOfDigitalDeliveryWorkflowActionHandlerTest {

	private EndOfDigitalDeliveryWorkflowActionHandler endOfDigitalDeliveryWorkflowActionHandler;
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
		times.setRecipientViewMaxTimeForDigital( Duration.ZERO );
		Mockito.when( pnDeliveryPushConfigs.getTimeParams() ).thenReturn( times );
		endOfDigitalDeliveryWorkflowActionHandler = new EndOfDigitalDeliveryWorkflowActionHandler( timelineDao, actionsPool, legalFactStore, pnDeliveryPushConfigs );
	}
	
	@Test
    void successSendCourtesyEmail() {
		ArgumentCaptor<PnExtChnEmailEvent> emailEventCaptor = ArgumentCaptor.forClass( PnExtChnEmailEvent.class );
		
		//Given
		Action action = newAction();
	    Notification notification = newNotification();

		NotificationPathChooseDetails details = newNotificationPathChooseDetails();
		List<DigitalAddress> addresses = details.getCourtesyAddresses();
		TimelineElement timelineElement = newTimelineElement( addresses );
	    Mockito.when( timelineDao.getTimelineElement( Mockito.anyString(), Mockito.anyString()) )
				.thenReturn( Optional.of( timelineElement ) );
	    
	    //When
	    endOfDigitalDeliveryWorkflowActionHandler.handleAction( action, notification );
	    
		//Then
		verify( actionsPool ).scheduleFutureAction( Mockito.any(Action.class) );
		
		verify( timelineDao ).addTimelineElement( Mockito.any(TimelineElement.class) );
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
				.actionId( "IUN_01_send_courtesy_rec0" )
				.type( ActionType.END_OF_DIGITAL_DELIVERY_WORKFLOW)
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
		                        .digitalDomicile(DigitalAddress.builder()
		                                .type( DigitalAddressType.PEC )
		                                .address( "nome1.cognome1@develop.it" )
		                                .build())
		                        .build()
		                        )
		        )
		        .build();
	}
}
