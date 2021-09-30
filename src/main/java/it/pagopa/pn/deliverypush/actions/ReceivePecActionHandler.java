package it.pagopa.pn.deliverypush.actions;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEventPayload;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEventPayloadDocument;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEventPayloadReceiver;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEventPayloadSender;
import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFailure;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFeedbackDetails;
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
public class ReceivePecActionHandler extends AbstractActionHandler {

	private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
	
	public ReceivePecActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, 
			PnDeliveryPushConfigs pnDeliveryPushConfigs, MomProducer<PnExtChnPaperEvent> paperRequestProducer) {
        super( timelineDao, actionsPool , pnDeliveryPushConfigs);
        this.paperRequestProducer = paperRequestProducer;
    }

    @Override
    public void handleAction(Action action, Notification notification ) {
        Action nextAction;
        PnExtChnProgressStatus status = action.getResponseStatus();
        NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
        boolean pecDeliveryFailure = false;
        	
        // - Se il messaggio è andato a buon fine schedula l'attesa
        if ( PnExtChnProgressStatus.OK.equals( status ) ) {
            nextAction = buildSendCourtesyAction(action);
        }
        // ... altrimenti continua
        else {
            nextAction = buildNextSendAction( action )
            				.orElse( buildSendPaperDeliveryRequestAction( action ) );
            
            // non ho ulteriori indirizzi/tentativi per invio PEC, invio richiesta via raccomandata e registro evento timeline definitivo fallimento notifica digitale
            if ( nextAction.getType().equals( ActionType.SEND_PAPER ) ) {
            	pecDeliveryFailure = true;
            	this.paperRequestProducer.push( buildSendPaperRequest( action, notification ) );
            }
        }

        scheduleAction( nextAction );

        Optional<NotificationPathChooseDetails> addresses =
                getTimelineElement( action, ActionType.CHOOSE_DELIVERY_MODE, NotificationPathChooseDetails.class );
        if( addresses.isPresent() ) {
            // - send pec if specific address present
            DigitalAddress address = action.getDigitalAddressSource().getAddressFrom(addresses.get());
            addTimelineElement(action, TimelineElement.builder()
                    .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE_FEEDBACK)
                    .details(new SendDigitalFeedbackDetails(SendDigitalDetails.sendBuilder()
                            .taxId(recipient.getTaxId())
                            .retryNumber(action.getRetryNumber())
                            .address( address )
                            .build(),
                            Collections.singletonList( status.name() )
                    ))
                    .build()
            );
            
            if ( pecDeliveryFailure ) {
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
        return ActionType.RECEIVE_PEC;
    }
}
