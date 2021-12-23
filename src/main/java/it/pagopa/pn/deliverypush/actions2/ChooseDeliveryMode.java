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
    private AnalogWorkflowHandler analogWorkflowHandler;

    /**
     * Get Recipient addresses and try to send notification in this order: PLATFORM, GENERAL, SPECIAL. Save availability information for all address in timeline
     *
     * @param notification Public Administration notification request
     * @param recipient    Notification recipient
     */
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

    /**
     * Insert timeline element with availability information for passed source
     *
     * @param taxId       User identifier
     * @param iun         iun Notification unique identifier
     * @param source      Source address PLATFORM, GENERAL, SPECIAL
     * @param isAvailable is address available
     */
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


    /**
     * Getting special address need async call. The method generate new correlationId start get information.
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    private void sendRequestForGetSpecialAddress(String iun, String taxId) {
        String correlationId = iun + "_" + taxId + "_" + "choose";
        publicRegistrySender.sendRequestForGetAddress(iun, taxId, correlationId, null, ContactPhase.CHOOSE_DELIVERY);
    }


    /**
     * Get special address response. If address is available Start Digital workflow with it else there isn't any digital address
     * available so start analog workflow
     *
     * @param response Response for get special address
     * @param iun      Notification unique identifier
     * @param taxId    User identifier
     */
    public void handleSpecialAddressResponse(PublicRegistryResponse response, String iun, String taxId) {
        if (response.getDigitalAddress() != null) {
            DigitalAddress digitalAddress = DigitalAddress.builder()  //TODO Da modificare
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

    /**
     * if courtesy message has been sent to the user, it is necessary to wait 5 days (from sent message date) before start Analog workflow
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    private void startAnalogWorkflow(String iun, String taxId) {
        List<CourtesyMessage> courtesyMessages = courtesyMessageHandler.getCourtesyMessages(iun, taxId);
        if (!courtesyMessages.isEmpty()) {
            //TODO Ottenere eventualmente il courtesy message con data invio minore e schedulare a 5 gg da quella data
            CourtesyMessage courtesyMessage = courtesyMessages.get(0);
            Instant schedulingDate = courtesyMessage.getInsertDate().plus(5, ChronoUnit.DAYS);
            scheduler.schedulEvent(schedulingDate, ActionType.ANALOG_WORKFLOW);
        } else {
            analogWorkflowHandler.analogWorkflowHandler(iun, taxId);
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


    /**
     * Starting digital workflow sending notification information to external channel
     *
     * @param notification   Public Administration notification request
     * @param digitalAddress User address
     * @param recipient      Notification recipient
     * @param iun            Notification unique identifier
     */
    private void startDigitalWorkflow(Notification notification, DigitalAddress digitalAddress, NotificationRecipient recipient, String iun) {
        externalChannel.sendDigitalNotification(notification, digitalAddress, iun, recipient);
    }

}
