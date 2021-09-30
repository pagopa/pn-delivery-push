package it.pagopa.pn.deliverypush.actions;

import java.time.Instant;

import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEventPayload;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEventPayloadNotice;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.ReceivedPaperDeliveredNoticeDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

@Component
public class ReceiveRegisteredLetterDeliveredNoticeActionHandler extends AbstractActionHandler {

	private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    
	public ReceiveRegisteredLetterDeliveredNoticeActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
			PnDeliveryPushConfigs pnDeliveryPushConfigs, MomProducer<PnExtChnPaperEvent> paperRequestProducer) {
		super(timelineDao, actionsPool, pnDeliveryPushConfigs);
		this.paperRequestProducer = paperRequestProducer;
	}
    
    @Override
    public void handleAction( Action action, Notification notification ) {
    	Action nextAction = null;
        NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );

        this.paperRequestProducer.push( PnExtChnPaperEvent.builder()
                .header( StandardEventHeader.builder()
                        .iun( action.getIun() )
                        .eventId( action.getActionId() )
                        .eventType( EventType.SEND_PAPER_NOTICE.name() )
                        .publisher( EventPublisher.DELIVERY_PUSH.name() )
                        .createdAt( Instant.now() )
                        .build()
                    )
                .payload( PnExtChnPaperEventPayload.builder()
                		.avvisoRicezione( PnExtChnPaperEventPayloadNotice.builder()
                							.iun( action.getIun() )
                							.notificationAquisitionMode( "notificationAquisitionMode" )
                							.paperAquisitionMode( "paperAquisitionMode" )
                							.build()
                				)
                		.build()
                    )
                .build()
            );
        
        nextAction = buildSendCourtesyAction( action );	// END_OF_DIGITAL_DELIVERY_WORKFLOW
        scheduleAction( nextAction );
    	
    	// - Write timeline
        addTimelineElement( action, TimelineElement.builder()
                .category( TimelineElementCategory.RECEIVED_PAPER_DELIVERED_NOTICE )
                .details( ReceivedPaperDeliveredNoticeDetails.builder()
                			.address( recipient.getPhysicalAddress() )
                			.build() 
                )
                .build()
        );
    }

    @Override
    public ActionType getActionType() {
        return ActionType.RECEIVE_PAPER;
    }

}
