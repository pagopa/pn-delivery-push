package it.pagopa.pn.deliverypush.actions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.DigitalAddressSource;

@Component
public class SendCourtesyMessagesActionHandler extends AbstractActionHandler {

    private final MomProducer<PnExtChnEmailEvent> emailRequestProducer;
    private final ActionsPool actionsPool;
    private final LegalFactUtils legalFactStore;

    public SendCourtesyMessagesActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, 
    		PnDeliveryPushConfigs pnDeliveryPushConfigs, MomProducer<PnExtChnEmailEvent> emailRequestProducer, LegalFactUtils legalFactStore) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.emailRequestProducer = emailRequestProducer;
        this.actionsPool = actionsPool;
        this.legalFactStore = legalFactStore;
    }

    @Override
    public void handleAction(Action action, Notification notification) {
    	// - Retrieve addresses
    	Optional<NotificationPathChooseDetails> addresses =
                getTimelineElement( action, ActionType.CHOOSE_DELIVERY_MODE, NotificationPathChooseDetails.class );
    	
    	List<Action> actionsToLegalAct = new ArrayList<>();
    	
    	for ( DigitalAddressSource addressSource : DigitalAddressSource.values() ) {
    		for (int rn = 1; rn <= 2; rn ++) {
    			Action possibleReceiveAction = Action.builder()
    											.type( ActionType.RECEIVE_PEC )
    											.iun( action.getIun() )
    											.recipientIndex( action.getRecipientIndex() )
    											.digitalAddressSource( addressSource )
    											.retryNumber( rn )
    											.build();
    			
    			String actionId = ActionType.RECEIVE_PEC.buildActionId( possibleReceiveAction );
    			
    			Optional<Action> sentAction = actionsPool.loadActionById( actionId );
        	    
        	    if ( sentAction.isPresent() ) {
        	    	actionsToLegalAct.add( sentAction.get() );
        	    }
        	    
    		}
    	}
    	
    	legalFactStore.groupLegalFacts( actionsToLegalAct, notification, addresses );
    	
        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());

        if (addresses.isPresent()) {
            NotificationPathChooseDetails addressesValue = addresses.get();
            int numberOfAddresses;
            if (addressesValue.getCourtesyAddresses() != null) {
                numberOfAddresses = addresses.get().getCourtesyAddresses().size();
            } else {
                numberOfAddresses = 0;
            }

            for (int idx = 0; idx < numberOfAddresses; idx++) {
                DigitalAddress emailAddress = addresses.get().getCourtesyAddresses().get(idx);
                this.emailRequestProducer.push(PnExtChnEmailEvent.builder()
                        .header(StandardEventHeader.builder()
                                .iun(action.getIun())
                                .eventId(action.getActionId() + "_" + idx)
                                .eventType(EventType.SEND_COURTESY_EMAIL.name())
                                .publisher(EventPublisher.DELIVERY_PUSH.name())
                                .createdAt(Instant.now())
                                .build()
                        )
                        .payload(PnExtChnEmailEventPayload.builder()
                                .iun(notification.getIun())
                                .senderId(notification.getSender().getPaId())
                                .emailAddress(emailAddress.getAddress())
                                .build()
                        )
                        .build()
                );
            }

            // - GENERATE NEXT ACTIONS
            Action nextAction = buildWaitRecipientTimeoutAction(action);
            scheduleAction(nextAction);

            // - WRITE TIMELINE
            addTimelineElement(action, TimelineElement.builder()
                    .category(TimelineElementCategory.SEND_COURTESY_MESSAGE)
                    .details(SendCourtesyDetails.builder()
                            .taxId(recipient.getTaxId())
                            .addresses(addresses.get().getCourtesyAddresses())
                            .build()
                    )
                    .build()
            );

        }


    }

    @Override
    public ActionType getActionType() {
        return ActionType.SEND_COURTESY_MESSAGES;
    }
}
