package it.pagopa.pn.deliverypush.actions;

import java.util.Collections;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalFailureDetails;
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
	private final ExtChnEventUtils extChnEventUtils;
	
	public ReceivePecActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, 
			PnDeliveryPushConfigs pnDeliveryPushConfigs, MomProducer<PnExtChnPaperEvent> paperRequestProducer, ExtChnEventUtils extChnEventUtils) {
        super( timelineDao, actionsPool , pnDeliveryPushConfigs);
        this.paperRequestProducer = paperRequestProducer;
        this.extChnEventUtils = extChnEventUtils;
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
            				.orElse( buildSendPaperAction( action ) );
            
            // invio richiesta via raccomandata e registro evento timeline definitivo fallimento notifica digitale
            if ( nextAction.getType().equals( ActionType.SEND_PAPER ) ) {
            	pecDeliveryFailure = true;
            	this.paperRequestProducer.push( extChnEventUtils.buildSendPaperRequest( action, notification, 
            			CommunicationType.RECIEVED_DELIVERY_NOTICE, ServiceLevelType.SIMPLE_REGISTERED_LETTER ) );
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
        return ActionType.RECEIVE_PEC;
    }
}
