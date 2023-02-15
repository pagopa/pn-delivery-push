package it.pagopa.pn.deliverypush.action.choosedeliverymode;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendCourtesyMessageDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

@Component
@Slf4j
public class ChooseDeliveryModeHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final SchedulerService schedulerService;
    private final NationalRegistriesService nationalRegistriesService;
    private final ChooseDeliveryModeUtils chooseDeliveryUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final NotificationService notificationService;

    public ChooseDeliveryModeHandler(ChooseDeliveryModeUtils chooseDeliveryUtils,
                                     DigitalWorkFlowHandler digitalWorkFlowHandler,
                                     SchedulerService schedulerService,
                                     NationalRegistriesService nationalRegistriesService,
                                     PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                     NotificationService notificationService) {
        this.chooseDeliveryUtils = chooseDeliveryUtils;
        this.digitalWorkFlowHandler = digitalWorkFlowHandler;
        this.schedulerService = schedulerService;
        this.nationalRegistriesService = nationalRegistriesService;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.notificationService = notificationService;
    }

    /**
     * Handle notification type choice (DIGITAL or ANALOG)
     * Get Recipient addresses for user and try to send notification in this order: PLATFORM, SPECIAL, GENERAL.
     * Save availability information for all address in timeline
     *
     * @param iun Notification unique identifier
     * @param recIndex User identifier
     */
    public void chooseDeliveryTypeAndStartWorkflow(String iun, Integer recIndex) {
        log.info("Start ChooseDeliveryTypeAndStartWorkflow process - iun={} recipientIndex={}", iun, recIndex);

        NotificationInt notification = notificationService.getNotificationByIun(iun);
        
        Optional<LegalDigitalAddressInt> platformAddressOpt = chooseDeliveryUtils.getPlatformAddress(notification, recIndex);

        //Verifico presenza indirizzo di piattaforma, ...
        if (platformAddressOpt.isPresent()) {
            log.info("Platform address is present, Digital workflow can be started - iun={} recipientIndex={}", notification.getIun(), recIndex);
            LegalDigitalAddressInt platformAddress = platformAddressOpt.get();

            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.PLATFORM, true);
            digitalWorkFlowHandler.startDigitalWorkflow(notification, platformAddress, DigitalAddressSourceInt.PLATFORM, recIndex);
        } else {
            log.info("Platform address isn't present - iun={} recipientIndex={}", notification.getIun(), recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.PLATFORM, false);

            // ... se non lo trovo, verifico presenza indirizzo speciale, ...
            LegalDigitalAddressInt specialAddress = chooseDeliveryUtils.getDigitalDomicile(notification, recIndex);
            if (specialAddress != null && StringUtils.hasText(specialAddress.getAddress())) {
                log.info("Special address is present, Digital workflow can be started  - iun={} id={}", notification.getIun(), recIndex);

                chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.SPECIAL, true);
                digitalWorkFlowHandler.startDigitalWorkflow(notification, specialAddress, DigitalAddressSourceInt.SPECIAL, recIndex);
            } else {
                log.info("Special address isn't present, need to get General address async - iun={} recipientIndex={}", notification.getIun(), recIndex);
                chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.SPECIAL, false);

                // ... se non lo trovo, lancio ricerca asincrona dell'indirizzo generale
                nationalRegistriesService.sendRequestForGetDigitalGeneralAddress(notification, recIndex, ContactPhaseInt.CHOOSE_DELIVERY, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER);
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
            digitalWorkFlowHandler.startDigitalWorkflow(notification, response.getDigitalAddress(), DigitalAddressSourceInt.GENERAL, recIndex);
        } else {
            log.info("General address is not present, digital workflow can't be started. Starting Analog Workflow  - iun={} id={}", notification.getIun(), recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.GENERAL, false);
            scheduleAnalogWorkflow(notification, recIndex);
        }
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
            log.info("Courtesy message is present, need to schedule analog workflow at={} courtesySendDate is {}- iun={} id={} ", schedulingDate, sendDate, iun, recIndex);
        } else {
            schedulingDate = Instant.now();
            log.info("Courtesy message is not present, analog workflow can be started now  - iun={} id={} ", iun, recIndex);
        }
        chooseDeliveryUtils.addScheduleAnalogWorkflowToTimeline(recIndex, notification);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.ANALOG_WORKFLOW);
    }
}
