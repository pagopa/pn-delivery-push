package it.pagopa.pn.deliverypush.actions;
/*
import java.util.Collections;
import java.util.Optional;

import it.pagopa.pn.deliverypush.middleware.timelinedao.TimelineDao;
import org.springframework.stereotype.Component;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import NotificationPathChooseDetails;
import SendDigitalDetails;
import SendDigitalFeedbackDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;

@Component
public class ReceivePecActionHandler extends AbstractActionHandler {

	public ReceivePecActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                   PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        super( timelineDao, actionsPool , pnDeliveryPushConfigs);
    }

    @Override
    public void handleAction(Action action, Notification notification ) {
        Action nextAction;
        PnExtChnProgressStatus status = action.getResponseStatus();
        NotificationRecipient recipient = notification.getRecipients().get( action.getRecipientIndex() );

        // - Se il messaggio è andato a buon fine schedula l'attesa
        if ( PnExtChnProgressStatus.OK.equals( status ) ) {
            nextAction = buildEndofDigitalWorkflowAction(action);
        }
        // ... altrimenti continua
        else {
            nextAction = buildNextSendAction( action );
        }

        scheduleAction( nextAction );

        Optional<NotificationPathChooseDetails> addresses =
                getTimelineElement( action, ActionType.CHOOSE_DELIVERY_MODE, NotificationPathChooseDetails.class );
        if( addresses.isPresent() ) {
            DigitalAddress address = action.getDigitalAddressSource().getAddressFrom(addresses.get());
            addTimelineElement(action, TimelineElementInternal.timelineInternalBuilder()
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

        }else {
            throw new PnInternalException( "Addresses list not found!!! Needed for action " + action );
        }
    }

    @Override
    public ActionType getActionType() {
        return ActionType.RECEIVE_PEC;
    }
}


 */