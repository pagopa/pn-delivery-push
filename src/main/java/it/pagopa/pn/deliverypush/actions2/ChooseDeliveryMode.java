package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.notification.CourtesyMessage.CourtesyMessage;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.GetAddressInfo;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class ChooseDeliveryMode {
    private AddressBook addressBook;
    private ExternalChannel externalChannel;
    private PublicRegistryHandler publicRegistrySender;
    private TimelineService timelineService;
    private NotificationDao notificationDao;
    private CourtesyMessageHandler courtesyMessageHandler;
    private Scheduler scheduler;

    public void chooseDeliveryTypeAndStartWorkflow(Notification notification, NotificationRecipient recipient) {
        String taxId = recipient.getTaxId();
        String iun = notification.getIun();
        addressBook.getAddresses(taxId)
                .ifPresent(addressBookItem -> {
                    DigitalAddresses digitalAddresses = addressBookItem.getDigitalAddresses();

                    if (isAddressAvailable(digitalAddresses, digitalAddresses.getPlatform())) {
                        startDigitalWorkflow(notification, digitalAddresses.getPlatform(), recipient, iun);
                        addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.PLATFORM, true);
                    } else {
                        addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.PLATFORM, false);

                        if (isAddressAvailable(digitalAddresses, digitalAddresses.getGeneral())) {
                            startDigitalWorkflow(notification, digitalAddresses.getGeneral(), recipient, iun);
                            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.GENERAL, true);
                        } else {
                            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.GENERAL, false);
                            sendRequestForGetSpecialAddress(notification.getIun(), taxId);
                        }
                    }
                });
    }

    private boolean isAddressAvailable(DigitalAddresses digitalAddresses, DigitalAddress platform) {
        return digitalAddresses != null && platform != null && platform.getAddress() != null;
    }

    private void addAvailabilitySourceToTimeline(String taxId, String iun, DigitalAddressSource2 source, boolean isAvailable) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.GET_ADDRESS)
                .iun(iun)
                .details(GetAddressInfo.builder()
                        .taxId(taxId)
                        .source(source)
                        .isAvailable(isAvailable)
                        .build())
                .build());
    }

    private void sendRequestForGetSpecialAddress(String iun, String taxId) {
        String correlationId = iun + "_" + taxId + "_" + "choose";
        publicRegistrySender.sendNotification(iun, taxId, correlationId, null, ContactPhase.CHOOSE_DELIVERY);
    }

    public void handleSpecialAddressResponse(PublicRegistryResponse response, String iun, String taxId) {
        if (response.getDigitalAddress() != null) {
            DigitalAddress digitalAddress = DigitalAddress.builder()
                    .address(response.getDigitalAddress())
                    .type(DigitalAddressType.PEC)
                    .build();

            getNotificationAndStartDigitalWorkflow(digitalAddress, iun, taxId);
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.SPECIAL, true);
        } else {
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.SPECIAL, false);
            startAnalogWorkflow(iun, taxId);
        }
    }

    private void startAnalogWorkflow(String iun, String taxId) {
        List<CourtesyMessage> courtesyMessages = courtesyMessageHandler.getCourtesyMessages(iun, taxId);
        if (!courtesyMessages.isEmpty()) {
            //TODO Ottenere eventualmente il courtesy message con data invio minore e schedulare a 5 gg da quella data
            CourtesyMessage courtesyMessage = courtesyMessages.get(0);
            Instant schedulingDate = courtesyMessage.getInsertDate().plus(5, ChronoUnit.DAYS);
            scheduler.schedulEvent(schedulingDate, ActionType.ANALOG_WORKFLOW);
        } else {
            //TODO Inizializzare il workflow analogico
        }
    }

    private void getNotificationAndStartDigitalWorkflow(DigitalAddress digitalAddress, String iun, String taxId) {
        Optional<Notification> optNotification = notificationDao.getNotificationByIun(iun);
        if (optNotification.isPresent()) {
            Notification notification = optNotification.get();
            NotificationRecipient recipient = null; //TODO Ottiene il recipient dalla notifica
            startDigitalWorkflow(notification, digitalAddress, recipient, iun);
        } else {
            //TODO Gestire casistica di errore
        }
    }

    private void startDigitalWorkflow(Notification notification, DigitalAddress digitalAddress, NotificationRecipient recipient, String iun) {
        externalChannel.sendDigitalNotification(notification, digitalAddress, iun, recipient);
    }

}
