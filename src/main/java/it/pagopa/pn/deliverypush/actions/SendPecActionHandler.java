package it.pagopa.pn.deliverypush.actions;
/*
import java.util.Optional;

import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import NotificationPathChooseDetails;
import SendDigitalDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

@Component
public class SendPecActionHandler extends AbstractActionHandler {

    private final MomProducer<PnExtChnPecEvent> pecRequestProducer;
    private final ExtChnEventUtils extChnEventUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    public SendPecActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                MomProducer<PnExtChnPecEvent> pecRequestProducer, PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                ExtChnEventUtils extChnEventUtils) {
        super( timelineDao, actionsPool , pnDeliveryPushConfigs);
        this.pecRequestProducer = pecRequestProducer;
        this.extChnEventUtils = extChnEventUtils;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    @Override
    public void handleAction(Action action, Notification notification ) {

        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());

        // - Retrieve addresses
        Optional<NotificationPathChooseDetails> addresses =
                getTimelineElement( action, ActionType.CHOOSE_DELIVERY_MODE, NotificationPathChooseDetails.class );

        if( addresses.isPresent() ) {

            // - send pec if specific address present
            DigitalAddress address = action.getDigitalAddressSource().getAddressFrom( addresses.get() );
            if( address != null ) {
                this.pecRequestProducer.push( extChnEventUtils.buildSendPecRequest(
                        action,
                        notification,
                        recipient,
                        address) );
            }
            //   else go to next address (if this is not last)
            else {
            	Action nextAction = buildNextSendAction( action );
                scheduleAction( nextAction );
            }
  
            // - Write timeline
            addTimelineElement( action, TimelineElementInternal.timelineInternalBuilder()
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE )
                .details( SendDigitalDetails.sendBuilder()
                            .taxId( recipient.getTaxId() )
                            .address( address )
                            .retryNumber( action.getRetryNumber() )
                            .build()
                    )
                .build()
            );
        }
        else {
            throw new PnInternalException( "Addresses list not found!!! Needed for action " + action );
        }

    }
    
    @Override
    public ActionType getActionType() {
        return ActionType.SEND_PEC;
    }
}

 */
