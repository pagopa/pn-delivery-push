package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.action.utils.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendCourtesyMessageDetailsInt;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class ChooseDeliveryModeHandler {
    private final ExternalChannelService externalChannelService;
    private final SchedulerService schedulerService;
    private final PublicRegistryService publicRegistryService;
    private final ChooseDeliveryModeUtils chooseDeliveryUtils;
    private final InstantNowSupplier instantNowSupplier;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public ChooseDeliveryModeHandler(ChooseDeliveryModeUtils chooseDeliveryUtils,
                                     ExternalChannelService externalChannelService, 
                                     SchedulerService schedulerService,
                                     PublicRegistryService publicRegistryService,
                                     InstantNowSupplier instantNowSupplier,
                                     PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.chooseDeliveryUtils = chooseDeliveryUtils;
        this.externalChannelService = externalChannelService;
        this.schedulerService = schedulerService;
        this.publicRegistryService = publicRegistryService;
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
        log.info("Start ChooseDeliveryTypeAndStartWorkflow process - iun={} recipientIndex={}", notification.getIun(), recIndex);

        Optional<LegalDigitalAddressInt> platformAddressOpt = chooseDeliveryUtils.getPlatformAddress(notification, recIndex);

        //Verifico presenza indirizzo di piattaforma, ...
        if (platformAddressOpt.isPresent()) {
            log.info("Platform address is present, Digital workflow can be started - iun={} recipientIndex={}", notification.getIun(), recIndex);
            LegalDigitalAddressInt platformAddress = platformAddressOpt.get();

            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.PLATFORM, true);
            startDigitalWorkflow(notification, platformAddress, DigitalAddressSourceInt.PLATFORM, recIndex);
        } else {
            log.info("Platform address isn't present - iun={} recipientIndex={}", notification.getIun(), recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.PLATFORM, false);

            // ... se non lo trovo, verifico presenza indirizzo speciale, ...
            LegalDigitalAddressInt specialAddress = chooseDeliveryUtils.getDigitalDomicile(notification, recIndex);
            if (specialAddress != null && StringUtils.hasText(specialAddress.getAddress())) {
                log.info("Special address is present, Digital workflow can be started  - iun={} id={}", notification.getIun(), recIndex);

                startDigitalWorkflow(notification, specialAddress, DigitalAddressSourceInt.SPECIAL, recIndex);
                chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.SPECIAL, true);
            } else {
                log.info("Special address isn't present, need to get General address async - iun={} recipientIndex={}", notification.getIun(), recIndex);
                chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.SPECIAL, false);

                // ... se non lo trovo, lancio ricerca asincrona dell'indirizzo generale
                publicRegistryService.sendRequestForGetDigitalGeneralAddress(notification, recIndex, ContactPhaseInt.CHOOSE_DELIVERY, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER);
            }
        }

        log.info("END chooseDeliveryTypeAndStartWorkflow process - iun={} recipientIndex={}", notification.getIun(), recIndex);
    }

    /**
     * Handle Get general address response. If address is available Start Digital workflow else there isn't any digital address
     * available, in this case analog workflow will be started
     *
     * @param response Response for get general address
     * @param notification      Notification
     * @param recIndex    User identifier
     */
    public void handleGeneralAddressResponse(PublicRegistryResponse response, NotificationInt notification, Integer recIndex) {
        log.info("HandleGeneralAddressResponse in choose phase  - iun={} id={}", notification.getIun(), recIndex);

        if (response.getDigitalAddress() != null) {
            log.info("General address is present, Digital workflow can be started  - iun={} id={}", notification.getIun(), recIndex);

            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.GENERAL, true);
            startDigitalWorkflow(notification, response.getDigitalAddress(), DigitalAddressSourceInt.GENERAL, recIndex);
        } else {
            log.info("General address is not present, digital workflow can't be started. Starting Analog Workflow  - iun={} id={}", notification.getIun(), recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.GENERAL, false);
            scheduleAnalogWorkflow(notification, recIndex);
        }
    }

    /**
     * Starting digital workflow sending notification information to external channel
     *
     * @param notification   Public Administration notification request
     * @param digitalAddress User address
     * @param addressSource Address source ( PLATFORM, SPECIAL, GENERAL );
     * @param recIndex      User identifier
     */
    public void startDigitalWorkflow(NotificationInt notification, LegalDigitalAddressInt digitalAddress, DigitalAddressSourceInt addressSource, Integer recIndex) {
        log.info("Starting digital workflow sending notification to external channel - iun={} id={} ", notification.getIun(), recIndex);
        externalChannelService.sendDigitalNotification(notification, digitalAddress, addressSource, recIndex, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER);
    }

    /**
     * Start analog workflow, if courtesy message has been sent to the user, it is necessary to wait 5 days (from sent message date) before start Analog workflow
     *
     * @param notification   Notification
     * @param recIndex User identifier
     */
    public void scheduleAnalogWorkflow(NotificationInt notification, Integer recIndex) {
        String iun = notification.getIun();
        log.debug("Scheduling analog workflow for iun={} id={} ", iun, recIndex);

        Optional<SendCourtesyMessageDetailsInt> sendCourtesyMessageDetailsOpt = chooseDeliveryUtils.getFirstSentCourtesyMessage(iun, recIndex);
        Instant schedulingDate;

        if (sendCourtesyMessageDetailsOpt.isPresent()) {
            SendCourtesyMessageDetailsInt sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            Instant sendDate = sendCourtesyMessageDetails.getSendDate();
            
            schedulingDate = sendDate.plus(pnDeliveryPushConfigs.getTimeParams().getWaitingForReadCourtesyMessage());//5 Days
            log.info("Courtesy message is present, need to schedule analog workflow at={}  - iun={} id={} ", schedulingDate, iun, recIndex);
        } else {
            schedulingDate = instantNowSupplier.get();
            log.info("Courtesy message is not present, analog workflow can be started now  - iun={} id={} ", iun, recIndex);
        }
        chooseDeliveryUtils.addScheduleAnalogWorkflowToTimeline(recIndex, notification);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.ANALOG_WORKFLOW);
    }
}
