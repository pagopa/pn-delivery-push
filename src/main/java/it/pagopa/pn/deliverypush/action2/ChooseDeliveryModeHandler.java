package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource2;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyMessageDetails;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@Slf4j
public class ChooseDeliveryModeHandler {
    public static final int START_SENT_ATTEMPT_NUMBER = 0;

    private final AddressBookService addressBookService;
    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final ExternalChannelService externalChannelService;
    private final CourtesyMessageService courtesyMessageService;
    private final SchedulerService schedulerService;
    private final PublicRegistryService publicRegistryService;

    public ChooseDeliveryModeHandler(AddressBookService addressBookService, TimelineService timelineService, NotificationService notificationService,
                                     ExternalChannelService externalChannelService, CourtesyMessageService courtesyMessageService, SchedulerService schedulerService,
                                     PublicRegistryService publicRegistryService) {
        this.addressBookService = addressBookService;
        this.timelineService = timelineService;
        this.notificationService = notificationService;
        this.externalChannelService = externalChannelService;
        this.courtesyMessageService = courtesyMessageService;
        this.schedulerService = schedulerService;
        this.publicRegistryService = publicRegistryService;
    }

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
        DigitalAddress platformAddress = addressBookService.retrievePlatformAddress(recipient, notification.getSender());

        //Verifico presenza indirizzo di piattaforma, ...
        if (platformAddress != null) {
            log.debug("Platform address is present, Digital workflow can be started");
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.PLATFORM, true);
            startDigitalWorkflow(notification, platformAddress, DigitalAddressSource2.PLATFORM, recipient);
        } else {
            log.debug("Platform address isn't present");
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.PLATFORM, false);

            // ... se non lo trovo, verifico presenza indirizzo speciale, ...
            DigitalAddress specialAddress = recipient.getDigitalDomicile();
            if (specialAddress != null) {
                log.debug("Special address is present, Digital workflow can be started");

                startDigitalWorkflow(notification, specialAddress, DigitalAddressSource2.SPECIAL, recipient);
                addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.SPECIAL, true);
            } else {
                log.debug("Special address isn't present, need to get General address async");
                addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.SPECIAL, false);

                // ... se non lo trovo, lancio ricerca asincrona dell'indirizzo generale
                publicRegistryService.sendRequestForGetAddress(iun, taxId, null, ContactPhase.CHOOSE_DELIVERY, START_SENT_ATTEMPT_NUMBER);
            }
        }

        log.debug("END chooseDeliveryTypeAndStartWorkflow process for IUN {} id {}", notification.getIun(), recipient.getTaxId());
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

            Notification notification = notificationService.getNotificationByIun(iun);
            NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);
            log.debug("Notification and recipient successfully obtained");

            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.GENERAL, true);
            startDigitalWorkflow(notification, response.getDigitalAddress(), DigitalAddressSource2.GENERAL, recipient);
        } else {
            log.debug("General address is not present, digital workflow can't be started. Starting Analog Workflow");
            addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource2.GENERAL, false);
            scheduleAnalogWorkflow(iun, taxId);
        }
    }

    /**
     * Starting digital workflow sending notification information to external channel
     *
     * @param notification   Public Administration notification request
     * @param digitalAddress User address
     * @param recipient      Notification recipient
     */
    public void startDigitalWorkflow(Notification notification, DigitalAddress digitalAddress, DigitalAddressSource2 addressSource, NotificationRecipient recipient) {
        log.info("Starting digital workflow for IUN {} id {} sending notification to external channel", notification.getIun(), recipient.getTaxId());
        externalChannelService.sendDigitalNotification(notification, digitalAddress, addressSource, recipient, START_SENT_ATTEMPT_NUMBER);
    }

    /**
     * Start analog workflow, if courtesy message has been sent to the user, it is necessary to wait 5 days (from sent message date) before start Analog workflow
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    public void scheduleAnalogWorkflow(String iun, String taxId) {
        log.info("Start analog workflow for iun {} id {} ", iun, taxId);

        Optional<SendCourtesyMessageDetails> sendCourtesyMessageDetailsOpt = courtesyMessageService.getFirstSentCourtesyMessage(iun, taxId);
        Instant schedulingDate;

        if (sendCourtesyMessageDetailsOpt.isPresent()) {
            SendCourtesyMessageDetails sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            schedulingDate = sendCourtesyMessageDetails.getSendDate().plus(5, ChronoUnit.DAYS);
            log.info("Courtesy message is present, need to schedule analog workflow at {}", schedulingDate);
        } else {
            schedulingDate = Instant.now();
            log.info("Courtesy message is not present, analog workflow can be started now");
        }
        schedulerService.schedulEvent(iun, taxId, schedulingDate, ActionType.ANALOG_WORKFLOW);
    }
    
    private void addAvailabilitySourceToTimeline(String taxId, String iun, DigitalAddressSource2 special, boolean isAvailable) {
        timelineService.addAvailabilitySourceToTimeline(taxId, iun, special, isAvailable, START_SENT_ATTEMPT_NUMBER);
    }
}
