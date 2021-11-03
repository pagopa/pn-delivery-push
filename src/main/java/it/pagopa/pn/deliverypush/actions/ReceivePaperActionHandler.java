package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class ReceivePaperActionHandler extends AbstractActionHandler {



	public ReceivePaperActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                     PnDeliveryPushConfigs pnDeliveryPushConfigs) {
		super(timelineDao, actionsPool, pnDeliveryPushConfigs);
	}
    
    @Override
    public void handleAction( Action action, Notification notification ) {
    	PnExtChnProgressStatus status = action.getResponseStatus();
        NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );

        Action nextAction = null;

        switch (status) {
            case OK: {
                nextAction = buildEndofAnalogWorkflowAction( action );	// END_OF_ANALOG_DELIVERY_WORKFLOW
            } break;
            case PERMANENT_FAIL:
            case RETRYABLE_FAIL: {
                nextAction = buildNextPaperSendAction( action );
            } break;

            default: throw new PnInternalException("Status not supported: " + status);
        }

        scheduleAction( nextAction );

        // recuperare timeline azione di spedizione corrispondente
        Optional<SendPaperDetails> sendDetails = super.getTimelineElement(
                action,
                ActionType.SEND_PAPER,
                SendPaperDetails.class);

        PhysicalAddress address;
        if(sendDetails.isPresent()) {
            address = sendDetails.get().getAddress();
        } else {
            throw new PnInternalException( "Send Timeline related to " + action + " not found!!! " );
        }
    	
    	// - Write timeline
        addTimelineElement( action, TimelineElement.builder()
                .category( TimelineElementCategory.SEND_PAPER_FEEDBACK )
                .details( new SendPaperFeedbackDetails (
                            SendPaperDetails.builder()
                                .taxId( recipient.getTaxId() )
                			    .address( address )
                                .serviceLevel(sendDetails.get().getServiceLevel())
                			    .build(),
                            action.getNewPhysicalAddress(),
                            Collections.singletonList( status.name())
                ))
                .build()
        );
    }

    private Action buildNextPaperSendAction(Action action) {
        return action.toBuilder()
                .retryNumber(action.getRetryNumber() + 1 )
                .newPhysicalAddress( action.getNewPhysicalAddress() )
                .type( ActionType.SEND_PAPER )
                .build();
    }

    @Override
    public ActionType getActionType() {
        return ActionType.RECEIVE_PAPER;
    }

}
