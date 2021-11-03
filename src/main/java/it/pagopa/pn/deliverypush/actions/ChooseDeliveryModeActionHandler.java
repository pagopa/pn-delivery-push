package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.DeliveryMode;
import it.pagopa.pn.api.dto.notification.timeline.NotificationPathChooseDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.abstractions.MomProducer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
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
    private final ExtChnEventUtils eventUtils;


    public ChooseDeliveryModeActionHandler(TimelineDao timelineDao, AddressBook addressBook,
                                           ActionsPool actionsPool, PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                           MomProducer<PnExtChnEmailEvent> emailRequestProducer,
                                           ExtChnEventUtils eventUtils) {
        super( timelineDao, actionsPool , pnDeliveryPushConfigs);
        this.addressBook = addressBook;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.emailRequestProducer = emailRequestProducer;
        this.eventUtils = eventUtils;
    }

    @Override
    public void handleAction(Action action, Notification notification) {
        // - GET RECIPIENT
        int index = action.getRecipientIndex();
        NotificationRecipient recipient = notification.getRecipients().get( index );

        PhysicalAddress physicalAddress = recipient.getPhysicalAddress();

        // - LOAD ADDRESS BOOK and compute timeline
        NotificationPathChooseDetails.NotificationPathChooseDetailsBuilder timelineDetailsBuilder =
                NotificationPathChooseDetails.builder()
                .taxId( recipient.getTaxId() )
                .physicalAddress( physicalAddress )
                .special( recipient.getDigitalDomicile() )
                ;

        addressBook.getAddresses( recipient.getTaxId() )
            .ifPresent( abEntry -> {
                if(abEntry.getDigitalAddresses() != null) {
                    timelineDetailsBuilder
                            .general(abEntry.getDigitalAddresses().getGeneral())
                            .platform(abEntry.getDigitalAddresses().getPlatform());
                }
                if(physicalAddress == null) {
                    timelineDetailsBuilder.physicalAddress(abEntry.getResidentialAddress());
                }
                timelineDetailsBuilder.courtesyAddresses(abEntry.getCourtesyAddresses());

                sendCourtesyMessages(action, notification, recipient, abEntry);
            });

        NotificationPathChooseDetails timelineDetails = timelineDetailsBuilder.build();

        boolean analogDeliveryMode = isAnalogRecipient(timelineDetails);
        DeliveryMode deliveryMode = analogDeliveryMode ? DeliveryMode.ANALOG : DeliveryMode.DIGITAL;

        Action.ActionBuilder actionBuilder = Action.builder()
                .iun(action.getIun())
                .recipientIndex(action.getRecipientIndex())
                .retryNumber( 1 )
                .notBefore(Instant.now().plus(pnDeliveryPushConfigs.getTimeParams().getWaitingForNextAction()));


        switch (deliveryMode) {
            case DIGITAL: {
                super.scheduleAction( actionBuilder
                        .type( ActionType.SEND_PEC )
                        .digitalAddressSource( DigitalAddressSource.PLATFORM )
                        .build()
                );
            } break;
            case ANALOG: {
                super.scheduleAction( actionBuilder
                        .type(ActionType.SEND_PAPER)
                        .build()
                );
            } break;
            default: throw new PnInternalException("Delivery mode not supported: " + deliveryMode);
        }


        // - WRITE TIMELINE
        super.addTimelineElement( action, TimelineElement.builder()
                .category( TimelineElementCategory.NOTIFICATION_PATH_CHOOSE )
                .details( timelineDetails.toBuilder()
                        .deliveryMode( deliveryMode )
                        .build() )
                .build()
            );
    }

    private boolean isAnalogRecipient(NotificationPathChooseDetails timelineDetails) {
        return timelineDetails.getGeneral() == null &&
                timelineDetails.getPlatform() == null &&
                timelineDetails.getSpecial() == null;
    }

    private void sendCourtesyMessages(Action action,
                                      Notification notification,
                                      NotificationRecipient recipient,
                                      AddressBookEntry abEntry) {
        // - Send Email
        List<DigitalAddress> courtesyAddresses = abEntry.getCourtesyAddresses();
        if( courtesyAddresses != null ) {
            int numberOfAddresses = courtesyAddresses.size();

            for (int idx = 0; idx < numberOfAddresses; idx++) {
                DigitalAddress emailAddress = courtesyAddresses.get(idx);
                this.emailRequestProducer.push(
                        eventUtils.buildSendEmailRequest(action,
                                notification,
                                recipient,
                                idx,
                                emailAddress,
                                pnDeliveryPushConfigs));
            }
        }
    }


    @Override
    public ActionType getActionType() {
        return ActionType.CHOOSE_DELIVERY_MODE;
    }
}
