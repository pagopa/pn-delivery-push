package it.pagopa.pn.deliverypush.actions;

import java.util.Collections;

import org.springframework.stereotype.Component;

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

@Component
public class PecFailReceivePaperFeedbackActionHandler extends AbstractActionHandler {
    
	public PecFailReceivePaperFeedbackActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                                    PnDeliveryPushConfigs pnDeliveryPushConfigs) {
		super(timelineDao, actionsPool, pnDeliveryPushConfigs);
	}
    
    @Override
    public void handleAction( Action action, Notification notification ) {
    	PnExtChnProgressStatus status = action.getResponseStatus();
        NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );

        Action nextAction = buildEndofDigitalWorkflowAction( action );	// END_OF_DIGITAL_DELIVERY_WORKFLOW
        scheduleAction( nextAction );
    	
    	// - Write timeline
        addTimelineElement( action, TimelineElement.builder()
                .category( TimelineElementCategory.SEND_PAPER_FEEDBACK )
                .details( new SendPaperFeedbackDetails (
                            SendPaperDetails.builder()
                                .taxId( recipient.getTaxId() )
                			    .address( recipient.getPhysicalAddress() )
                                .serviceLevel(PecFailSendPaperActionHandler.DIGITAL_FAILURE_PAPER_FALLBACK_SERVICE_LEVEL)
                			    .build(),
                            action.getNewPhysicalAddress(),
                            action.getAttachmentKeys(),
                            Collections.singletonList( status.name())
                ))
                .build()
        );
    }

    @Override
    public ActionType getActionType() {
        return ActionType.PEC_FAIL_RECEIVE_PAPER;
    }

}
