package it.pagopa.pn.deliverypush.actions;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.legalfacts.DigitalAdviceReceiptLegalFact;
import it.pagopa.pn.api.dto.legalfacts.DigitalAdviceReceiptLegalFact.OkOrFail;
import it.pagopa.pn.api.dto.legalfacts.RecipientInfo;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFeedbackDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

@Component
public class ReceivePecActionHandler extends AbstractActionHandler {

	private final LegalFactUtils legalFactStore;
	
    public ReceivePecActionHandler(LegalFactUtils legalFactStore, TimelineDao timelineDao, ActionsPool actionsPool , PnDeliveryPushConfigs pnDeliveryPushConfigs ) {
        super( timelineDao, actionsPool , pnDeliveryPushConfigs);
        this.legalFactStore = legalFactStore;
    }

    @Override
    public void handleAction(Action action, Notification notification ) {

        Action nextAction;
        PnExtChnProgressStatus status = action.getResponseStatus();
        NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );
        
        Optional<NotificationPathChooseDetails> addresses =
                getTimelineElement( action, ActionType.CHOOSE_DELIVERY_MODE, NotificationPathChooseDetails.class );
        DigitalAddress address = null;
        
        if( addresses.isPresent() ) {
        	address = action.getDigitalAddressSource().getAddressFrom( addresses.get() );
        	
	        // - WRITE LEGAL FACTS
	        legalFactStore.saveLegalFact( action.getIun(), "sent_pec_" + recipient.getTaxId(),
	        		DigitalAdviceReceiptLegalFact.builder()
	        			.iun( notification.getIun() )
	        			.date( legalFactStore.instantToDate( Instant.now() ) )
	        			.outcome( PnExtChnProgressStatus.OK.equals( status ) ? OkOrFail.OK : OkOrFail.FAIL )
	        			.recipient( RecipientInfo.builder()
	                            		.taxId( recipient.getTaxId() )
	                            		.denomination( recipient.getDenomination() )
	                            		.build()
	                    )
	        			.digitalAddress( address.getAddress() )
	        			.digitalAddressType( address.getType() )
	        			.build()
	        );
        } else {
            throw new PnInternalException( "Digital Addresses not found!!! Cannot generate digital advice receipt" );
        }
        
        // - Se il messaggio è andato a buon fine schedula l'attesa
        if ( PnExtChnProgressStatus.OK.equals( status ) ) {
            nextAction = buildSendCourtesyAction(action);
        }
        // ... altrimenti continua
        else {
            nextAction = buildNextSendAction( action ).orElse( buildSendCourtesyAction(action) );
        }

        scheduleAction( nextAction );

        if( addresses.isPresent() ) {

            // - send pec if specific address present
            // DigitalAddress address = action.getDigitalAddressSource().getAddressFrom(addresses.get());
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
        }
        else {
            throw new PnInternalException( "Addresses list not found!!! Needed for action " + action );
        }
    }

    @Override
    public ActionType getActionType() {
        return ActionType.RECEIVE_PEC;
    }
}
