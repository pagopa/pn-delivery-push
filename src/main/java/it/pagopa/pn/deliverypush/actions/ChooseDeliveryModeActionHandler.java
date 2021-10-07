package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionsPool;
import it.pagopa.pn.deliverypush.abstractions.actionspool.DigitalAddressSource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ChooseDeliveryModeActionHandler extends AbstractActionHandler {

    private final AddressBook addressBook;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final MomProducer<PnExtChnEmailEvent> emailRequestProducer;

    public ChooseDeliveryModeActionHandler(TimelineDao timelineDao, AddressBook addressBook,
                      ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                           MomProducer<PnExtChnEmailEvent> emailRequestProducer) {
        super( timelineDao, actionsPool , pnDeliveryPushConfigs);
        this.addressBook = addressBook;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.emailRequestProducer = emailRequestProducer;
    }

    @Override
    public void handleAction(Action action, Notification notification) {
        // - GET RECIPIENT
        int index = action.getRecipientIndex();
        NotificationRecipient recipient = notification.getRecipients().get( index );

        // - LOAD ADDRESS BOOK and compute timeline
        NotificationPathChooseDetails.NotificationPathChooseDetailsBuilder timelineDetailsBuilder =
                NotificationPathChooseDetails.builder()
                .taxId( recipient.getTaxId() )
                .deliveryMode( DeliveryMode.DIGITAL )
                .physicalAddress( null )
                .special( recipient.getDigitalDomicile() )
                ;

        addressBook.getAddresses( recipient.getTaxId() )
            .ifPresent( abEntry -> {
                timelineDetailsBuilder
                        .general(abEntry.getDigitalAddresses().getGeneral())
                        .platform(abEntry.getDigitalAddresses().getPlatform())
                        .courtesyAddresses(abEntry.getCourtesyAddresses());

                // - Send Email
                List<DigitalAddress> courtesyAddresses = abEntry.getCourtesyAddresses();
                if( courtesyAddresses != null ) {
                    int numberOfAddresses = courtesyAddresses.size();

                    for (int idx = 0; idx < numberOfAddresses; idx++) {
                        DigitalAddress emailAddress = courtesyAddresses.get(idx);
                        this.emailRequestProducer.push(PnExtChnEmailEvent.builder()
                                .header(StandardEventHeader.builder()
                                        .iun(action.getIun())
                                        .eventId(action.getActionId() + "_" + idx)
                                        .eventType(EventType.SEND_COURTESY_EMAIL.name())
                                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                                        .createdAt(Instant.now())
                                        .build()
                                )
                                .payload(PnExtChnEmailEventPayload.builder()
                                        .iun(notification.getIun())
                                        .senderId(notification.getSender().getPaId())
                                        .senderDenomination("NOT HANDLED FROM in PoC: Id=" + notification.getSender().getPaId())
                                        .senderEmailAddress("Not required")
                                        .recipientDenomination(recipient.getDenomination())
                                        .recipientTaxId(recipient.getTaxId())
                                        .emailAddress(emailAddress.getAddress())
                                        .shipmentDate(notification.getSentAt())
                                        .build()
                                )
                                .build()
                        );
                    }
                }

            });

        NotificationPathChooseDetails timelineDetails = timelineDetailsBuilder.build();

        // - GENERATE NEXT ACTIONS (choose digital)
        super.scheduleAction( Action.builder()
                .iun( action.getIun() )
                .recipientIndex( action.getRecipientIndex() )
                .type( ActionType.SEND_PEC )
                .digitalAddressSource( DigitalAddressSource.PLATFORM )
                .retryNumber( 1 )
                .notBefore( Instant.now().plus(pnDeliveryPushConfigs.getTimeParams().getWaitingForNextAction()) )
                .build()
            );

        // - WRITE TIMELINE (choose digital)
        super.addTimelineElement( action, TimelineElement.builder()
                .category( TimelineElementCategory.NOTIFICATION_PATH_CHOOSE )
                .details( timelineDetails )
                .build()
            );
    }
        
    @Override
    public ActionType getActionType() {
        return ActionType.CHOOSE_DELIVERY_MODE;
    }
}
