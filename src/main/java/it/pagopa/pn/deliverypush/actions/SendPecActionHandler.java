package it.pagopa.pn.deliverypush.actions;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEventPayload;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEventPayload;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFailureDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

@Component
public class SendPecActionHandler extends AbstractActionHandler {

    private final MomProducer<PnExtChnPecEvent> pecRequestProducer;
    private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private final ExtChnEventUtils extChnEventUtils;
    
    public SendPecActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, 
    		MomProducer<PnExtChnPecEvent> pecRequestProducer, PnDeliveryPushConfigs pnDeliveryPushConfigs,
    		MomProducer<PnExtChnPaperEvent> paperRequestProducer, ExtChnEventUtils extChnEventUtils) {
        super( timelineDao, actionsPool , pnDeliveryPushConfigs);
        this.pecRequestProducer = pecRequestProducer;
        this.paperRequestProducer = paperRequestProducer;
        this.extChnEventUtils = extChnEventUtils;
    }

    @Override
    public void handleAction(Action action, Notification notification ) {

        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
        boolean isPecDeliverable = false;
        
        // - Retrieve addresses
        Optional<NotificationPathChooseDetails> addresses =
                getTimelineElement( action, ActionType.CHOOSE_DELIVERY_MODE, NotificationPathChooseDetails.class );

        if( addresses.isPresent() ) {

            // - send pec if specific address present
            DigitalAddress address = action.getDigitalAddressSource().getAddressFrom( addresses.get() );
            if( address != null ) {
                this.pecRequestProducer.push( extChnEventUtils.buildSendPecRequest(action, notification, recipient, address) );
            }
            //   else go to next address (if this is not last)
            else {
            	Action nextAction = buildNextSendAction( action )
                        .orElse( buildSendPaperAction( action ) );
            	
            	// invio richiesta via raccomandata e registro evento timeline definitivo fallimento notifica digitale
            	if ( nextAction.getType().equals( ActionType.SEND_PAPER ) ) {
                	isPecDeliverable = true;
                	this.paperRequestProducer.push( extChnEventUtils.buildSendPaperRequest( action, notification, 
                			CommunicationType.RECIEVED_DELIVERY_NOTICE, ServiceLevelType.SIMPLE_REGISTERED_LETTER ) );
                }
            	
                scheduleAction( nextAction );
            }
  
            if ( isPecDeliverable ) {
            	// - Write timeline
            	addTimelineElement( action, TimelineElement.builder()
                    .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE )
                    .details( SendDigitalDetails.sendBuilder()
                            	.taxId( recipient.getTaxId() )
                            	.address( address )
                            	.retryNumber( action.getRetryNumber() )
                            	.build()
                    	)
                    .build()
                );
            } else {
                // - WRITE TIMELINE IN CASE OF PEC PERMANENT DELIVER FAILURE
            	addTimelineElement(action, TimelineElement.builder()
                        .category( TimelineElementCategory.SEND_DIGITAL_DOMICILE_FAILURE )
                        .details( SendDigitalFailureDetails.builder()
                        			.address( address )
                        			.build()
                        )
                        .build()
                );
            }
        }
        else {
            throw new PnInternalException( "Addresses list not found!!! Needed for action " + action );
        }

    }
    
    @Override
    public ActionType getActionType() {
        return ActionType.SEND_PEC;
    }
}
