package it.pagopa.pn.deliverypush.actions;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
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
import lombok.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SendPaperActionHandler extends AbstractActionHandler {

    private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private final ExtChnEventUtils extChnEventUtils;
    private final AddressBook addressBook;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public SendPaperActionHandler(TimelineDao timelineDao, ActionsPool actionsPool,
                                  PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                  MomProducer<PnExtChnPaperEvent> paperRequestProducer,
                                  ExtChnEventUtils extChnEventUtils,
                                  AddressBook addressBook) {
        super(timelineDao, actionsPool, pnDeliveryPushConfigs);
        this.paperRequestProducer = paperRequestProducer;
        this.extChnEventUtils = extChnEventUtils;
        this.addressBook = addressBook;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    @Override
    public void handleAction(Action action, Notification notification) {

        NotificationRecipient recipient = notification.getRecipients().get(action.getRecipientIndex());
        PhysicalAddress addressFromPA = recipient.getPhysicalAddress();

        Optional<SendPolicy> optionalSendPolicy;

        switch (action.getRetryNumber()) {
            case 1: {
                //address da PA?
                if (addressFromPA != null) {
                    // send paper 890 o Racc A/R con indagine
                    optionalSendPolicy = Optional.of(new SendPolicy(addressFromPA, true));
                } else {
                    // - Retrieve addresses from Action Choose_delivery_mode
                    Optional<PhysicalAddress> nationalRegistryAddress = retrieveNationalRegistryAddress(recipient.getTaxId());
                    if (nationalRegistryAddress.isPresent()) {
                        optionalSendPolicy = Optional.of(new SendPolicy(nationalRegistryAddress.get(), true));
                    } else {
                        optionalSendPolicy = setForCompletelyUnreachable();
                    }
                }
            }
            break;
            case 2: {
                // secondo tentativo
                SendPaperFeedbackDetails previousAttempt = retrievePreviousAttempt(action);
                Optional<PhysicalAddress> nationalRegistryAddress = retrieveNationalRegistryAddress(recipient.getTaxId());
                Optional<PhysicalAddress> destination = chooseDestination(previousAttempt, nationalRegistryAddress);
                if (destination.isPresent()) {
                    optionalSendPolicy = Optional.of(new SendPolicy(destination.get(), false));
                } else {
                    optionalSendPolicy = setForCompletelyUnreachable();
                }
            }
            break;
            case 3: {
                optionalSendPolicy = setForCompletelyUnreachable();
            }
            break;

            default:
                throw new PnInternalException("" + action.getRetryNumber());
        }

        if (optionalSendPolicy.isPresent()) {
            final SendPolicy sendPolicy = optionalSendPolicy.get();
            final boolean investigation = sendPolicy.isInvestigation();
            final PhysicalAddress destination = sendPolicy.getDestination();

            final PnExtChnPaperEvent event = extChnEventUtils.buildSendPaperRequest(
                    action,
                    notification,
                    CommunicationType.RECIEVED_DELIVERY_NOTICE,
                    notification.getPhysicalCommunicationType(),
                    investigation,
                    destination);
            this.paperRequestProducer.push(event);

            // - Write timeline
            addTimelineElement(action, TimelineElement.builder()
                    .category(TimelineElementCategory.SEND_ANALOG_DOMICILE)
                    .details(SendPaperDetails.builder()
                            .taxId(recipient.getTaxId())
                            .address(destination)
                            .serviceLevel(event.getPayload().getServiceLevel())
                            .investigation(investigation)
                            .build()
                    ).build());
        } else {
            scheduleCompletlyUnreachableAction(action);
        }

    }

    //Irreperibile Totale
    private Optional<SendPolicy> setForCompletelyUnreachable() {
        return Optional.empty();
    }

    private void scheduleCompletlyUnreachableAction(Action action) {
        Action nextAction = buildCompletelyUnreachableAction(action);
        scheduleAction(nextAction);
    }

    private Optional<PhysicalAddress> chooseDestination(
            SendPaperFeedbackDetails previousAttempt,
            Optional<PhysicalAddress> nationalRegistryAddress) {
        if (nationalRegistryAddress.isPresent() &&
                !nationalRegistryAddress.get().equals(previousAttempt.getAddress())) {
            return Optional.of(nationalRegistryAddress.get());
        } else {
            return Optional.ofNullable(previousAttempt.getNewAddress());
        }
    }

    private SendPaperFeedbackDetails retrievePreviousAttempt(Action action) {

        final Integer retryNumber = action.getRetryNumber();
        if (retryNumber < 2) {
            throw new PnInternalException("Unable to retrieve previous attempt for action " + action);
        }
        Optional<SendPaperFeedbackDetails> sendPaperDetails = super.getTimelineElement(
                action.toBuilder()
                        .type(ActionType.RECEIVE_PAPER)
                        .retryNumber(retryNumber - 1)
                        .build(),
                ActionType.RECEIVE_PAPER,
                SendPaperFeedbackDetails.class);

        if (sendPaperDetails.isPresent()) {
            return sendPaperDetails.get();
        } else {
            throw new PnInternalException("Send Timeline related to " + action + " not found!!! ");
        }
    }

    private Optional<PhysicalAddress> retrieveNationalRegistryAddress(String taxId) {
        Optional<AddressBookEntry> optionalAddressBookEntry = addressBook.getAddresses(taxId);
        if (optionalAddressBookEntry.isPresent()) {
            return Optional.ofNullable(optionalAddressBookEntry.get().getResidentialAddress());
        }
        return Optional.empty();
    }

    @Value
    private static class SendPolicy {
        private PhysicalAddress destination;
        private boolean investigation;
    }


    @Override
    public ActionType getActionType() {
        return ActionType.SEND_PAPER;
    }

}
