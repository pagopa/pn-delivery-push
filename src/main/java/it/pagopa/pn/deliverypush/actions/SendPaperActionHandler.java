package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SendPaperActionHandler extends AbstractActionHandler {

    private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private final TimelineDao timelineDao;
    private final ExtChnEventUtils extChnEventUtils;

    public SendPaperActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                  PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                  MomProducer<PnExtChnPaperEvent> paperRequestProducer,
                                  ExtChnEventUtils extChnEventUtils
    ) {
		super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.paperRequestProducer = paperRequestProducer;
        this.timelineDao = timelineDao;
        this.extChnEventUtils = extChnEventUtils;
    }
    
    @Override
    public void handleAction( Action action, Notification notification ) {
        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
        PnExtChnProgressStatus status = action.getResponseStatus();

        // SECONDO TENTATIVO DI INVIO
        if(status != null && status.equals(PnExtChnProgressStatus.RETRYABLE_FAIL)) {
            recipient = recipient.toBuilder()
                    .physicalAddress(action.getNewPhysicalAddress())
                    .build();
            action = action.toBuilder()
                    .retryNumber( 2 )
                    .build();
        }

        //TODO

        this.paperRequestProducer.push( extChnEventUtils.buildSendPaperRequest(
                action,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,
                notification.getPhysicalCommunicationType()
            ));


        // - Write timeline
        addTimelineElement( action, TimelineElement.builder()
                .category(TimelineElementCategory.SEND_PAPER )
                .details( SendPaperDetails.builder()
                        .taxId( recipient.getTaxId() )
                        .address( recipient.getPhysicalAddress() )
                        .build()
                )
                .build()
        );

    }

    @Override
    public ActionType getActionType() {
        return ActionType.SEND_PAPER;
    }

}
