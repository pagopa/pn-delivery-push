package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import org.springframework.stereotype.Component;

import java.util.Collections;

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

        Action nextAction = buildEndofDigitalWorkflowAction( action );	// END_OF_ANALOG_DELIVERY_WORKFLOW
        scheduleAction( nextAction );
    	
    	// - Write timeline
        addTimelineElement( action, TimelineElement.builder()
                .category( TimelineElementCategory.SEND_PAPER_FEEDBACK )
                .details( new SendPaperFeedbackDetails (
                            SendPaperDetails.builder()
                                .taxId( recipient.getTaxId() )
                			    .address( recipient.getPhysicalAddress() )
                			    .build(),
                            Collections.singletonList( status.name())
                ))
                .build()
        );
    }

    @Override
    public ActionType getActionType() {
        return ActionType.RECEIVE_PAPER;
    }

}
