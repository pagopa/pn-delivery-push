package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.legalfacts.NotificationReceivedLegalFact;
import it.pagopa.pn.api.dto.legalfacts.RecipientInfoWithAddresses;
import it.pagopa.pn.api.dto.legalfacts.SenderInfo;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.timeline.ReceivedDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Component
public class SenderAckActionHandler extends AbstractActionHandler {

    private final LegalFactUtils legalFactStore;

    public SenderAckActionHandler(LegalFactUtils legalFactStore, TimelineDao timelineDao, ActionsPool actionsPool) {
        super( timelineDao, actionsPool );
        this.legalFactStore = legalFactStore;
    }

    @Override
    public void handleAction(Action action, Notification notification) {
        // - WRITE LEGAL FACTS
        for( NotificationRecipient recipient: notification.getRecipients() ) {
            legalFactStore.saveLegalFact( action.getIun(), "sender_ack_" + recipient.getTaxId(),
                    NotificationReceivedLegalFact.builder()
                            .iun( notification.getIun() )
                            .sender( SenderInfo.builder()
                                    .paTaxId( notification.getSender().getPaId() )
                                    .paDenomination( notification.getSender().getPaDenomination() )
                                    .build()
                            )
                            .date( legalFactStore.instantToDate( notification.getSentAt() ))
                            .recipient( RecipientInfoWithAddresses.builder()
                                    .taxId( recipient.getTaxId() )
                                    .denomination( recipient.getDenomination() )
                                    .digitalDomicile( recipient.getDigitalDomicile().getAddress() )
                                    .physicalDomicile( String.valueOf( recipient.getDigitalDomicile().toString() ) )
                                    .build()
                            )
                            .digests( notification.getDocuments()
                                    .stream()
                                    .map( d -> d.getDigests().getSha256() )
                                    .collect(Collectors.toList()) )
                            .build()
            );
        }

        // - GENERATE NEXT ACTIONS
        Instant notBefore = action.getNotBefore().plus( 1, ChronoUnit.SECONDS );
        int numberOfRecipients = notification.getRecipients().size();
        for( int idx = 0; idx < numberOfRecipients; idx ++ ) {
            Action nextAction = Action.builder()
                    .iun( action.getIun() )
                    .type( ActionType.CHOOSE_DELIVERY_MODE )
                    .notBefore( notBefore )
                    .recipientIndex( idx )
                    .build();
            scheduleAction( nextAction );
        }

        // - WRITE TIMELINE
        addTimelineElement( action, TimelineElement.builder()
                .category( TimelineElementCategory.RECEIVED_ACK )
                .details( ReceivedDetails.builder()
                        .recipients( notification.getRecipients() )
                        .documentsDigests( notification.getDocuments()
                                .stream()
                                .map( NotificationAttachment::getDigests )
                                .collect(Collectors.toList())
                        )
                        .build()
                )
                .build()
        );
    }

    @Override
    public ActionType getActionType() {
        return ActionType.SENDER_ACK;
    }
}
