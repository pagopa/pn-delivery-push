package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class ReceivePecActionHandler extends AbstractActionHandler {

    public ReceivePecActionHandler(TimelineDao timelineDao, ActionsPool actionsPool , TimeParams timeParams ) {
        super( timelineDao, actionsPool , timeParams);
    }

    @Override
    public void handleAction(Action action, Notification notification ) {

        Action nextAction;

        // - Se il messaggio è andato a buon fine schedula l'attesa
        PnExtChnProgressStatus status = action.getResponseStatus();
        if ( PnExtChnProgressStatus.OK.equals( status ) ) {
            nextAction = buildWaitRecipientTimeoutAction( action );
        }
        // ... altrimenti continua
        else {
            nextAction = buildNextSendAction( action ).orElse( buildWaitRecipientTimeoutAction( action ) );
        }

        scheduleAction( nextAction );


        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());

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
