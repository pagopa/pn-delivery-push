package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyMessageDetails;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.utils.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@Slf4j
public class ChooseDeliveryModeHandler {
    public static final int READ_COURTESY_MESSAGE_WAITING_TIME = 5;

    private final NotificationService notificationService;
    private final ExternalChannelSendHandler externalChannelSendHandler;
    private final SchedulerService schedulerService;
    private final PublicRegistrySendHandler publicRegistrySendHandler;
    private final ChooseDeliveryModeUtils chooseDeliveryUtils;
    private final InstantNowSupplier instantNowSupplier;

    public ChooseDeliveryModeHandler(ChooseDeliveryModeUtils chooseDeliveryUtils, NotificationService notificationService,
                                     ExternalChannelSendHandler externalChannelSendHandler, SchedulerService schedulerService,
                                     PublicRegistrySendHandler publicRegistrySendHandler, InstantNowSupplier instantNowSupplier) {
        this.chooseDeliveryUtils = chooseDeliveryUtils;
        this.notificationService = notificationService;
        this.externalChannelSendHandler = externalChannelSendHandler;
        this.schedulerService = schedulerService;
        this.publicRegistrySendHandler = publicRegistrySendHandler;
        this.instantNowSupplier = instantNowSupplier;
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
        log.info("Start ChooseDeliveryTypeAndStartWorkflow process-IUN {} id {}", notification.getIun(), recipient.getTaxId());

        String taxId = recipient.getTaxId();
        String iun = notification.getIun();
        DigitalAddress platformAddress = retrievePlatformAddress(recipient, notification.getSender());

        //Verifico presenza indirizzo di piattaforma, ...
        if (platformAddress != null) {
            log.info("Platform address is present, Digital workflow can be started - IUN {} id {}", notification.getIun(), recipient.getTaxId());
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource.PLATFORM, true);
            startDigitalWorkflow(notification, platformAddress, DigitalAddressSource.PLATFORM, recipient);
        } else {
            log.info("Platform address isn't present  - iun {} id {}", notification.getIun(), recipient.getTaxId());
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource.PLATFORM, false);

            // ... se non lo trovo, verifico presenza indirizzo speciale, ...
            DigitalAddress specialAddress = recipient.getDigitalDomicile();
            if (specialAddress != null) {
                log.info("Special address is present, Digital workflow can be started  - iun {} id {}", notification.getIun(), recipient.getTaxId());

                startDigitalWorkflow(notification, specialAddress, DigitalAddressSource.SPECIAL, recipient);
                chooseDeliveryUtils.addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource.SPECIAL, true);
            } else {
                log.info("Special address isn't present, need to get General address async");
                chooseDeliveryUtils.addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource.SPECIAL, false);

                // ... se non lo trovo, lancio ricerca asincrona dell'indirizzo generale
                publicRegistrySendHandler.sendRequestForGetDigitalAddress(iun, taxId, ContactPhase.CHOOSE_DELIVERY, ChooseDeliveryModeUtils.START_SENT_ATTEMPT_NUMBER);
            }
        }

        log.info("END chooseDeliveryTypeAndStartWorkflow process  - iun {} id {}", notification.getIun(), recipient.getTaxId());
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
        log.info("HandleGeneralAddressResponse in choose phase  - iun {} id {}", iun, taxId);

        if (response.getDigitalAddress() != null) {
            log.info("General address is present, Digital workflow can be started  - iun {} id {}", iun, taxId);

            Notification notification = notificationService.getNotificationByIun(iun);
            NotificationRecipient recipient = notificationService.getRecipientFromNotification(notification, taxId);
            log.debug("Notification and recipient successfully obtained  - iun {} id {}", iun, taxId);

            chooseDeliveryUtils.addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource.GENERAL, true);
            startDigitalWorkflow(notification, response.getDigitalAddress(), DigitalAddressSource.GENERAL, recipient);
        } else {
            log.info("General address is not present, digital workflow can't be started. Starting Analog Workflow  - iun {} id {}", iun, taxId);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(taxId, iun, DigitalAddressSource.GENERAL, false);
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
    public void startDigitalWorkflow(Notification notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, NotificationRecipient recipient) {
        log.info("Starting digital workflow sending notification to external channel - iun {} id {} ", notification.getIun(), recipient.getTaxId());
        externalChannelSendHandler.sendDigitalNotification(notification, digitalAddress, addressSource, recipient, ChooseDeliveryModeUtils.START_SENT_ATTEMPT_NUMBER);
    }

    /**
     * Start analog workflow, if courtesy message has been sent to the user, it is necessary to wait 5 days (from sent message date) before start Analog workflow
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    public void scheduleAnalogWorkflow(String iun, String taxId) {
        log.debug("Scheduling analog workflow for iun {} id {} ", iun, taxId);

        Optional<SendCourtesyMessageDetails> sendCourtesyMessageDetailsOpt = chooseDeliveryUtils.getFirstSentCourtesyMessage(iun, taxId);
        Instant schedulingDate;

        if (sendCourtesyMessageDetailsOpt.isPresent()) {
            SendCourtesyMessageDetails sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            schedulingDate = sendCourtesyMessageDetails.getSendDate().plus(READ_COURTESY_MESSAGE_WAITING_TIME, ChronoUnit.DAYS);
            log.info("Courtesy message is present, need to schedule analog workflow at {}  - iun {} id {} ", schedulingDate, iun, taxId);
        } else {
            schedulingDate = instantNowSupplier.get();
            log.info("Courtesy message is not present, analog workflow can be started now  - iun {} id {} ", iun, taxId);
        }
        schedulerService.scheduleEvent(iun, taxId, schedulingDate, ActionType.ANALOG_WORKFLOW);
    }


    private DigitalAddress retrievePlatformAddress(NotificationRecipient recipient, NotificationSender sender) {
        log.debug("retrievePlatformAddress  for id {}", recipient.getTaxId());

        Optional<AddressBookEntry> addressBookEntryOpt = chooseDeliveryUtils.getAddresses(recipient.getTaxId(), sender);

        if (addressBookEntryOpt.isPresent()) {
            DigitalAddresses digitalAddresses = addressBookEntryOpt.get().getDigitalAddresses(); //TODO Valutare se far ritornare un solo indirizzo all'addressbook e non una lista
            if (digitalAddresses != null) {
                DigitalAddress platformAddress = digitalAddresses.getPlatform();
                return platformAddress != null && platformAddress.getAddress() != null ? platformAddress : null;
            }
        }
        return null;
    }
}
