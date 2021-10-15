package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
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

import java.util.Optional;

@Component
public class SendPaperActionHandler extends AbstractActionHandler {

    private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private final ExtChnEventUtils extChnEventUtils;

    public SendPaperActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                  PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                  MomProducer<PnExtChnPaperEvent> paperRequestProducer,
                                  ExtChnEventUtils extChnEventUtils
    ) {
		super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.paperRequestProducer = paperRequestProducer;
        this.extChnEventUtils = extChnEventUtils;
    }
    
    @Override
    public void handleAction( Action action, Notification notification ) {

        PhysicalAddress address = retrievePhysicalAddress(action);

        this.paperRequestProducer.push( extChnEventUtils.buildSendPaperRequest(
                action,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,
                notification.getPhysicalCommunicationType(),
                address
            ));


        // - Write timeline
        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
        addTimelineElement( action, TimelineElement.builder()
                .category(TimelineElementCategory.SEND_PAPER )
                .details( SendPaperDetails.builder()
                        .taxId( recipient.getTaxId() )
                        .address( address )
                        .build()
                )
                .build()
        );

    }

    private PhysicalAddress retrievePhysicalAddress(Action action) {
        PhysicalAddress sendAddress;
        PhysicalAddress actionAddress = action.getNewPhysicalAddress();

        // PRIMO TENTATIVO DI INVIO
        if( actionAddress == null ) {
            // - Retrieve addresses
            Optional<NotificationPathChooseDetails> addresses =
                    getTimelineElement(action, ActionType.CHOOSE_DELIVERY_MODE, NotificationPathChooseDetails.class );
            if (addresses.isPresent()) {
                sendAddress = addresses.get().getPhysicalAddress();
            }
            else {
                throw new PnInternalException( "Addresses list not found!!! Needed for action " + action );
            }
        }
        // DAL SECONDO TENTATIVO
        else {
            sendAddress = actionAddress;
        }
        return sendAddress;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.SEND_PAPER;
    }

}
