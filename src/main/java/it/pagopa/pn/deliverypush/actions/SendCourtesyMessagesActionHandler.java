package it.pagopa.pn.deliverypush.actions;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnEmailEventPayload;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

@Component
public class SendCourtesyMessagesActionHandler extends AbstractActionHandler {
    
    private final MomProducer<PnExtChnEmailEvent> emailRequestProducer;

    public SendCourtesyMessagesActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, MomProducer<PnExtChnEmailEvent> emailRequestProducer) {
        super( timelineDao, actionsPool );
        this.emailRequestProducer = emailRequestProducer;
    }
    
    @Override
    public void handleAction(Action action, Notification notification ) {

    	NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
    	
    	// - Retrieve addresses
        Optional<NotificationPathChooseDetails> addresses =
                getTimelineElement( action, ActionType.CHOOSE_DELIVERY_MODE, NotificationPathChooseDetails.class );
              
        if( addresses.isPresent() ) {
        	int numberOfAddresses = addresses.get().getCourtesyAddresses().size();
        	 
        	for( int idx = 0; idx < numberOfAddresses; idx ++ ) {
        		DigitalAddress emailAddress = addresses.get().getCourtesyAddresses().get( idx );
        		this.emailRequestProducer.push( PnExtChnEmailEvent.builder()
        				.header( StandardEventHeader.builder()
        						.iun( action.getIun() )
        						.eventId( action.getActionId() )
        						.eventType( EventType.SEND_COURTESY_EMAIL.name() )
        						.publisher( EventPublisher.DELIVERY_PUSH.name() )
        						.createdAt( Instant.now() )
        						.build()
        					)
        				.payload( PnExtChnEmailEventPayload.builder()
        						.iun( notification.getIun() )
        						.senderId( notification.getSender().getPaId() )
        						.emailAddress( emailAddress.getAddress() )
        						.build()
        					)
        				.build()
        		  ); 		
        	}
        }
      
        // - GENERATE NEXT ACTIONS
        Action nextAction = buildWaitRecipientTimeoutAction(action);
        scheduleAction( nextAction );

        // - WRITE TIMELINE
        addTimelineElement( action, TimelineElement.builder()
                .category( TimelineElementCategory.SEND_COURTESY_MESSAGE )
                	.details( SendCourtesyDetails.builder()
                		.taxId( recipient.getTaxId() )
                		.addresses( recipient.getCourtesyAdresses() )
                        .build()
                )
                .build()
        );
    }

    @Override
    public ActionType getActionType() {
        return ActionType.SEND_COURTESY_MESSAGES;
    }
}
