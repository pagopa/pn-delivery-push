package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.GetAddressInfo;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook2;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ChooseDeliveryModeHandler {
    private AddressBook2 addressBook;
    private ExternalChannelService externalChannelService;
    private PublicRegistryService publicRegistryService;
    private TimelineService timelineService;
    private NotificationService notificationService;
    private AnalogWorkflowHandler analogWorkflowHandler;

    /**
     * Handle notification type choice (DIGITAL or ANALOG)
     * Get Recipient addresses for user and try to send notification in this order: PLATFORM, SPECIAL, GENERAL.
     * Save availability information for all address in timeline
     *
     * @param notification Public Administration notification request
     * @param recipient    Notification recipient
     */
    public void chooseDeliveryTypeAndStartWorkflow(Notification notification, NotificationRecipient recipient) {
        log.info("Start chooseDeliveryTypeAndStartWorkflow process for IUN {} id {}", notification.getIun(), recipient.getTaxId());

        String taxId = recipient.getTaxId();
        String iun = notification.getIun();
        DigitalAddress platformAddress = retrievePlatformAddress(recipient, notification.getSender());

        //Verifico presenza indirizzo di piattaforma, ...
        if (platformAddress != null) {
            log.debug("Platform address is present, Digital workflow can be started");

            startDigitalWorkflow(notification, platformAddress, recipient);
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.PLATFORM, true);
        } else {
            log.debug("Platform address isn't present");
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.PLATFORM, false);

            // ... se non lo trovo, verifico presenza indirizzo speciale, ...
            DigitalAddress specialAddress = recipient.getDigitalDomicile();
            if (specialAddress != null) {
                log.debug("Special address is present, Digital workflow can be started");

                startDigitalWorkflow(notification, specialAddress, recipient);
                addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.SPECIAL, true);
            } else {
                log.debug("Special address isn't present, need to get General address async");
                addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.SPECIAL, false);

                // ... se non lo trovo, lancio ricerca asincrona dell'indirizzo generale
                sendRequestForGetGeneralAddress(notification.getIun(), taxId);
            }
        }

        log.debug("END chooseDeliveryTypeAndStartWorkflow process for IUN {} id {}", notification.getIun(), recipient.getTaxId());
    }

    //TODO Questa logica è replicata in DIGITALWORFKLOW E CHOOSE DELIVERY PORTARLA A FATTOR COMUNE
    //Se il risultato è diverso da null allora i suoi campi sono diversi da null
    private DigitalAddress retrievePlatformAddress(NotificationRecipient recipient, NotificationSender sender) {

        Optional<AddressBookEntry> addressBookEntryOpt = addressBook.getAddresses(recipient.getTaxId(), sender);

        if (addressBookEntryOpt.isPresent()) {
            DigitalAddresses digitalAddress = addressBookEntryOpt.get().getDigitalAddresses(); //TODO Valutare se far ritornare un solo indirizzo all'addressbook e non una lista
            DigitalAddress platformAddress = digitalAddress.getPlatform();
            return platformAddress != null && platformAddress.getAddress() != null ? platformAddress : null;
        }
        return null;
    }

    /**
     * Insert availability information in timeline for user
     *
     * @param taxId       User identifier
     * @param iun         iun Notification unique identifier
     * @param source      Source address PLATFORM, SPECIAL, GENERAL
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
     * Send request for get GENERAL address (async call). The method generate new correlationId start get information.
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    private void sendRequestForGetGeneralAddress(String iun, String taxId) {
        String correlationId = iun + "_" + taxId + "_" + "choose";
        log.debug("Start sendRequestForGetGeneralAddress correlationId {}", correlationId);

        publicRegistryService.sendRequestForGetAddress(iun, taxId, correlationId, null, ContactPhase.CHOOSE_DELIVERY);
    }


    /**
     * Handle Get general address response. If address is available Start Digital workflow else there isn't any digital address
     * available, in this case analog workflow will be started
     *
     * @param response Response for get general address
     * @param iun      Notification unique identifier
     * @param taxId    User identifier
     */
    public void handleGeneralAddressResponse(PublicRegistryResponse response, String iun, String taxId) {
        log.debug("Start handleGeneralAddressResponse in choose phase");

        if (response.getDigitalAddress() != null) {
            log.debug("General address is present, Digital workflow can be started");

            startDigitalWorkflow(response.getDigitalAddress(), iun, taxId);
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.GENERAL, true);
        } else {
            log.debug("General address is not present, digital workflow can't be started. Starting Analog Workflow");
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.GENERAL, false);
            analogWorkflowHandler.startAnalogWorkflow(iun, taxId);
        }
    }

    private void startDigitalWorkflow(DigitalAddress digitalAddress, String iun, String taxId) {
        Notification notification = notificationService.getNotificationByIun(iun);
        NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);
        log.debug("Notification and recipient successfully obtained");

        startDigitalWorkflow(notification, digitalAddress, recipient);
    }


    /**
     * Starting digital workflow sending notification information to external channel
     *
     * @param notification   Public Administration notification request
     * @param digitalAddress User address
     * @param recipient      Notification recipient
     */
    private void startDigitalWorkflow(Notification notification, DigitalAddress digitalAddress, NotificationRecipient recipient) {
        log.info("Starting digital workflow for IUN {} id {} sending notification to external channel", notification.getIun(), recipient.getTaxId());
        externalChannelService.sendDigitalNotification(notification, digitalAddress, recipient);
    }

}
