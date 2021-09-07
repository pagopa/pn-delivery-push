package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class SendPecActionHandler extends AbstractActionHandler {

    private final MomProducer<PnExtChnPecEvent> pecRequestProducer;

    public SendPecActionHandler(TimelineDao timelineDao, ActionsPool actionsPool, MomProducer<PnExtChnPecEvent> pecRequestProducer, TimeParams timeParams) {
        super( timelineDao, actionsPool , timeParams);
        this.pecRequestProducer = pecRequestProducer;
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
                this.pecRequestProducer.push( PnExtChnPecEvent.builder()
                        .header( StandardEventHeader.builder()
                                .iun( action.getIun() )
                                .eventId( action.getActionId() )
                                .eventType( EventType.SEND_PEC_REQUEST.name() )
                                .publisher( EventPublisher.DELIVERY_PUSH.name() )
                                .createdAt( Instant.now() )
                                .build()
                            )
                        .payload( PnExtChnPecEventPayload.builder()
                                .iun( notification.getIun() )
                                .requestCorrelationId( action.getActionId() )
                                .codiceAtto( notification.getPaNotificationId() )
                                .recipientTaxId( recipient.getTaxId() )
                                .recipientDenomination( recipient.getDenomination() )
                                .senderId( notification.getSender().getPaId() )
                                .pecAddress( address.getAddress() )
                                .build()
                            )
                        .build()
                    );
            }
            //   else go to next address (if this is not last)
            else {
                Action nextAction = buildNextSendAction( action )
                        .orElse( buildWaitRecipientTimeoutAction( action ));

                scheduleAction( nextAction );
            }

            // - Write timeline
            addTimelineElement( action, TimelineElement.builder()
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
