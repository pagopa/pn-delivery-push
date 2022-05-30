package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action2.utils.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ContactPhase;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendCourtesyMessageDetails;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class ChooseDeliveryModeHandler {
    private final NotificationService notificationService;
    private final ExternalChannelSendHandler externalChannelSendHandler;
    private final SchedulerService schedulerService;
    private final PublicRegistrySendHandler publicRegistrySendHandler;
    private final ChooseDeliveryModeUtils chooseDeliveryUtils;
    private final InstantNowSupplier instantNowSupplier;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public ChooseDeliveryModeHandler(ChooseDeliveryModeUtils chooseDeliveryUtils, NotificationService notificationService,
                                     ExternalChannelSendHandler externalChannelSendHandler, SchedulerService schedulerService,
                                     PublicRegistrySendHandler publicRegistrySendHandler, InstantNowSupplier instantNowSupplier,
                                     PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.chooseDeliveryUtils = chooseDeliveryUtils;
        this.notificationService = notificationService;
        this.externalChannelSendHandler = externalChannelSendHandler;
        this.schedulerService = schedulerService;
        this.publicRegistrySendHandler = publicRegistrySendHandler;
        this.instantNowSupplier = instantNowSupplier;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    /**
     * Handle notification type choice (DIGITAL or ANALOG)
     * Get Recipient addresses for user and try to send notification in this order: PLATFORM, SPECIAL, GENERAL.
     * Save availability information for all address in timeline
     *
     * @param notification Public Administration notification request
     */
    public void chooseDeliveryTypeAndStartWorkflow(NotificationInt notification, Integer recIndex) {
        log.info("Start ChooseDeliveryTypeAndStartWorkflow process-IUN {} id {}", notification.getIun(), recIndex);

        String iun = notification.getIun();
        Optional<LegalDigitalAddressInt> platformAddressOpt = chooseDeliveryUtils.getPlatformAddress(notification, recIndex);

        //Verifico presenza indirizzo di piattaforma, ...
        if (platformAddressOpt.isPresent()) {
            log.info("Platform address is present, Digital workflow can be started - IUN {} id {}", notification.getIun(), recIndex);
            LegalDigitalAddressInt platformAddress = platformAddressOpt.get();
            
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, iun, DigitalAddressSource.PLATFORM, true);
            startDigitalWorkflow(notification, platformAddress, DigitalAddressSource.PLATFORM, recIndex);
        } else {
            log.info("Platform address isn't present  - iun {} id {}", notification.getIun(), recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, iun, DigitalAddressSource.PLATFORM, false);

            // ... se non lo trovo, verifico presenza indirizzo speciale, ...
            LegalDigitalAddressInt specialAddress = chooseDeliveryUtils.getDigitalDomicile(notification, recIndex);
            if (specialAddress != null && specialAddress.getAddress() != null) {
                log.info("Special address is present, Digital workflow can be started  - iun {} id {}", notification.getIun(), recIndex);

                startDigitalWorkflow(notification, specialAddress, DigitalAddressSource.SPECIAL, recIndex);
                chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, iun, DigitalAddressSource.SPECIAL, true);
            } else {
                log.info("Special address isn't present, need to get General address async");
                chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, iun, DigitalAddressSource.SPECIAL, false);

                // ... se non lo trovo, lancio ricerca asincrona dell'indirizzo generale
                publicRegistrySendHandler.sendRequestForGetDigitalGeneralAddress(notification, recIndex, ContactPhase.CHOOSE_DELIVERY, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER);
            }
        }

        log.info("END chooseDeliveryTypeAndStartWorkflow process  - iun {} id {}", notification.getIun(), recIndex);
    }

    /**
     * Handle Get general address response. If address is available Start Digital workflow else there isn't any digital address
     * available, in this case analog workflow will be started
     *
     * @param response Response for get general address
     * @param iun      Notification unique identifier
     * @param recIndex    User identifier
     */
    public void handleGeneralAddressResponse(PublicRegistryResponse response, String iun, Integer recIndex) {
        log.info("HandleGeneralAddressResponse in choose phase  - iun {} id {}", iun, recIndex);

        if (response.getDigitalAddress() != null) {
            log.info("General address is present, Digital workflow can be started  - iun {} id {}", iun, recIndex);

            NotificationInt notification = notificationService.getNotificationByIun(iun);
            
            log.debug("Notification and recipient successfully obtained  - iun {} id {}", iun, recIndex);

            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, iun, DigitalAddressSource.GENERAL, true);
            startDigitalWorkflow(notification, response.getDigitalAddress(), DigitalAddressSource.GENERAL, recIndex);
        } else {
            log.info("General address is not present, digital workflow can't be started. Starting Analog Workflow  - iun {} id {}", iun, recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, iun, DigitalAddressSource.GENERAL, false);
            scheduleAnalogWorkflow(iun, recIndex);
        }
    }

    /**
     * Starting digital workflow sending notification information to external channel
     *
     * @param notification   Public Administration notification request
     * @param digitalAddress User address
     * @param recIndex      User identifier
     */
    public void startDigitalWorkflow(NotificationInt notification, LegalDigitalAddressInt digitalAddress, DigitalAddressSource addressSource, Integer recIndex) {
        log.info("Starting digital workflow sending notification to external channel - iun {} id {} ", notification.getIun(), recIndex);
        externalChannelSendHandler.sendDigitalNotification(notification, digitalAddress, addressSource, recIndex, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER);
    }

    /**
     * Start analog workflow, if courtesy message has been sent to the user, it is necessary to wait 5 days (from sent message date) before start Analog workflow
     *
     * @param iun   Notification unique identifier
     * @param recIndex User identifier
     */
    public void scheduleAnalogWorkflow(String iun, Integer recIndex) {
        log.debug("Scheduling analog workflow for iun {} id {} ", iun, recIndex);

        Optional<SendCourtesyMessageDetails> sendCourtesyMessageDetailsOpt = chooseDeliveryUtils.getFirstSentCourtesyMessage(iun, recIndex);
        Instant schedulingDate;

        if (sendCourtesyMessageDetailsOpt.isPresent()) {
            SendCourtesyMessageDetails sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            Instant sendDate = sendCourtesyMessageDetails.getSendDate();
            
            schedulingDate = sendDate.plus(pnDeliveryPushConfigs.getTimeParams().getWaitingForReadCourtesyMessage());//5 Days
            log.info("Courtesy message is present, need to schedule analog workflow at {}  - iun {} id {} ", schedulingDate, iun, recIndex);
        } else {
            schedulingDate = instantNowSupplier.get();
            log.info("Courtesy message is not present, analog workflow can be started now  - iun {} id {} ", iun, recIndex);
        }
        chooseDeliveryUtils.addScheduleAnalogWorkflowToTimeline(recIndex, iun);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.ANALOG_WORKFLOW);
    }
}
