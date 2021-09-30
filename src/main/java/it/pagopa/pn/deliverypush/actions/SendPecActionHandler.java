package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFailure;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class SendPecActionHandler extends AbstractActionHandler {

    private final MomProducer<PnExtChnPecEvent> pecRequestProducer;
    private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    
    public SendPecActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, 
    		MomProducer<PnExtChnPecEvent> pecRequestProducer, PnDeliveryPushConfigs pnDeliveryPushConfigs,
    		MomProducer<PnExtChnPaperEvent> paperRequestProducer) {
        super( timelineDao, actionsPool , pnDeliveryPushConfigs);
        this.pecRequestProducer = pecRequestProducer;
        this.paperRequestProducer = paperRequestProducer;
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
                this.pecRequestProducer.push( PnExtChnPecEvent.builder()
                        .header( StandardEventHeader.builder()
                                .iun( action.getIun() )
                                .eventId( action.getActionId() )
                                .eventType( EventType.SEND_PEC_REQUEST.name() )
                                .publisher( EventPublisher.DELIVERY_PUSH.name() )
                                .createdAt( Instant.now() )
                                .build()
                            )
                        .payload( PnExtChnPecEventPayload.builder()
                                .iun( notification.getIun() )
                                .requestCorrelationId( action.getActionId() )
                                .recipientTaxId( recipient.getTaxId() )
                                .recipientDenomination( recipient.getDenomination() )
                                .senderId( notification.getSender().getPaId() )
                                .senderDenomination( "NOT HANDLED FROM in PoC: Id=" + notification.getSender().getPaId() )
                                .senderPecAddress("Not required")
                                .pecAddress( address.getAddress() )
                                .build()
                            )
                        .build()
                    );
            }
            //   else go to next address (if this is not last)
            else {
            	/* Action nextAction = buildNextSendAction( action )
                        .orElse( buildSendCourtesyAction(action) ); */
            	Action nextAction = buildNextSendAction( action )
                        .orElse( buildSendPaperDeliveryRequestAction( action ) );
            	
            	// non ho ulteriori indirizzi/tentativi per invio PEC, invio richiesta via raccomandata e registro evento timeline definitivo fallimento notifica digitale
            	if ( nextAction.getType().equals( ActionType.SEND_PAPER ) ) {
                	isPecDeliverable = true;
                	this.paperRequestProducer.push( buildSendPaperRequest( action, notification ) );
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
                        .details( SendDigitalFailure.builder()
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

    private PnExtChnPaperEvent buildSendPaperRequest (Action action, Notification notification) {
		NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
		
		return PnExtChnPaperEvent.builder()
		        .header( StandardEventHeader.builder()
		                	.iun( action.getIun() )
		                	.eventId( action.getActionId() )
		                	.eventType( EventType.SEND_PAPER_REQUEST.name() )
		                	.publisher( EventPublisher.DELIVERY_PUSH.name() )
		                	.createdAt( Instant.now() )
		                	.build()
		        )
		        .payload( PnExtChnPaperEventPayload.builder()
		    				.urlCallBack( "urlCallBack" )
		    				.documento( PnExtChnPaperEventPayloadDocument.builder()
		    								.iun( action.getIun() )
		    								.codiceAtto( "codiceAtto" )
		    								.numeroCronologico( 1 )
		    								.parteIstante( "parteIstante" )
		    								.procuratore( "procuratore" )
		    								.ufficialeGiudiziario( "ufficialeGiudiziario" )
		    								.build()
		    				)
		    				.mittente( PnExtChnPaperEventPayloadSender.builder()
		    							.paMittente( notification.getSender().getPaId() )
		    							.pecMittente( "pecMittente" )
		    							.build()
		    				)
		    				.destinatario( PnExtChnPaperEventPayloadReceiver.builder()
		    								.destinatario( recipient.getDenomination() )
		    								.codiceFiscale( recipient.getTaxId() )
		    								.presso( recipient.getPhysicalAddress().getAt() )
		    								.indirizzo( recipient.getPhysicalAddress().getAddress() )
		    								.specificaIndirizzo( "specificaIndirizzo" )
		    								.cap( recipient.getPhysicalAddress().getZip() )
		    								.comune( recipient.getPhysicalAddress().getMunicipality() )
		    								.provincia( recipient.getPhysicalAddress().getProvince() )
		    								.build()
		    				)
		    				.build()
		        )
		        .build();
	}
    
    @Override
    public ActionType getActionType() {
        return ActionType.SEND_PEC;
    }
}
