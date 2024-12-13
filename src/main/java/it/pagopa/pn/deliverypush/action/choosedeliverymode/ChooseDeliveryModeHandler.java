package it.pagopa.pn.deliverypush.action.choosedeliverymode;

import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ProbableDateAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChooseDeliveryModeHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final SchedulerService schedulerService;
    private final NationalRegistriesService nationalRegistriesService;
    private final ChooseDeliveryModeUtils chooseDeliveryUtils;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final FeatureEnabledUtils featureEnabledUtils;


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
        if (featureEnabledUtils.isPfNewWorkflowEnabled(notification.getSentAt())) {
            pfStartNewWorkflow(recIndex, notification);
        } else {
            pfStartOldWorkflow(recIndex, notification);
        }
        log.info("END chooseDeliveryTypeAndStartWorkflow process - iun={} recipientIndex={}", notification.getIun(), recIndex);
    }

    private void pfStartOldWorkflow(Integer recIndex, NotificationInt notification) {
        //Verifico presenza indirizzo di piattaforma, ...
        Optional<LegalDigitalAddressInt> platformAddressOpt = getPlatformAddress(notification, recIndex);
        // ... se non lo trovo, verifico presenza indirizzo speciale, ...
        if (platformAddressOpt.isEmpty()) {
            LegalDigitalAddressInt specialAddress = getSpecialAddress(notification, recIndex);
            if (specialAddress == null || !StringUtils.hasText(specialAddress.getAddress())) {
                // ... se non lo trovo, verifico presenza indirizzo generale
                pfStartNewWorkflow(recIndex, notification);
            }
        }
    }

    private void pfStartNewWorkflow(Integer recIndex, NotificationInt notification) {
        nationalRegistriesService.sendRequestForGetDigitalGeneralAddress(notification, recIndex, ContactPhaseInt.CHOOSE_DELIVERY, ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER, null);
    }

    private Optional<LegalDigitalAddressInt> getPlatformAddress(NotificationInt notification, Integer recIndex) {
        Optional<LegalDigitalAddressInt> platformAddressOpt = chooseDeliveryUtils.getPlatformAddress(notification, recIndex);
        if (platformAddressOpt.isPresent()) {
            log.info("Platform address is present, Digital workflow can be started - iun={} recipientIndex={}", notification.getIun(), recIndex);
            LegalDigitalAddressInt platformAddress = platformAddressOpt.get();
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.PLATFORM, true);
            digitalWorkFlowHandler.startDigitalWorkflow(notification, platformAddress, DigitalAddressSourceInt.PLATFORM, recIndex);
        } else {
            log.info("Platform address isn't present - iun={} recipientIndex={}", notification.getIun(), recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.PLATFORM, false);
        }
        return platformAddressOpt;
    }

    private LegalDigitalAddressInt getSpecialAddress(NotificationInt notification, Integer recIndex) {
        LegalDigitalAddressInt specialAddress = chooseDeliveryUtils.getDigitalDomicile(notification, recIndex);
        if (specialAddress != null && StringUtils.hasText(specialAddress.getAddress())) {
            log.info("Special address is present, Digital workflow can be started  - iun={} id={}", notification.getIun(), recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.SPECIAL, true);
            digitalWorkFlowHandler.startDigitalWorkflow(notification, specialAddress, DigitalAddressSourceInt.SPECIAL, recIndex);
        } else {
            log.info("Special address isn't present, need to get General address async - iun={} recipientIndex={}", notification.getIun(), recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.SPECIAL, false);
        }
        return specialAddress;
    }

    /**
     * Handle Get general address response. If address is available Start Digital workflow else there isn't any digital address
     * available, in this case analog workflow will be started
     *
     * @param response Response for get general address
     * @param notification      Notification
     * @param recIndex    User identifier
     */
    public void handleGeneralAddressResponse(NationalRegistriesResponse response, NotificationInt notification, Integer recIndex) {
        log.info("HandleGeneralAddressResponse in choose phase  - iun={} id={}", notification.getIun(), recIndex);

        if (response.getDigitalAddress() != null) {
            log.info("General address is present, Digital workflow can be started  - iun={} id={}", notification.getIun(), recIndex);

            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.GENERAL, true);
            digitalWorkFlowHandler.startDigitalWorkflow(notification, response.getDigitalAddress(), DigitalAddressSourceInt.GENERAL, recIndex);
        } else {
            log.info("General address is not present, digital workflow can't be started. Starting Analog Workflow  - iun={} id={}", notification.getIun(), recIndex);
            chooseDeliveryUtils.addAvailabilitySourceToTimeline(recIndex, notification, DigitalAddressSourceInt.GENERAL, false);
            if (featureEnabledUtils.isPfNewWorkflowEnabled(notification.getSentAt())) {
                log.info("New workflow is enabled - iun={} id={}", notification.getIun(), recIndex);
                generalAddressNotFoundStartNewWorkflow(notification, recIndex);
            } else {
                generalAddressNotFoundStartOldWorkflow(notification, recIndex);
            }
        }
    }

    private void generalAddressNotFoundStartOldWorkflow(NotificationInt notification, Integer recIndex) {
        scheduleAnalogWorkflow(notification, recIndex);
    }

    private void generalAddressNotFoundStartNewWorkflow(NotificationInt notification, Integer recIndex) {
        //Verifico presenza indirizzo speciale, ...
        LegalDigitalAddressInt specialAddress = getSpecialAddress(notification, recIndex);
        // ... se non lo trovo, verifico presenza indirizzo di piattaforma, ...
        if (specialAddress == null) {
            Optional<LegalDigitalAddressInt> platformAddressOpt = getPlatformAddress(notification, recIndex);
            // ... se non lo trovo, parte il flusso di invio notifica analogica.
            if (platformAddressOpt.isEmpty()) {
                generalAddressNotFoundStartOldWorkflow(notification, recIndex);
            }
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

        Instant schedulingDate = timelineService.getTimelineElementDetailForSpecificRecipient(notification.getIun(),
                        recIndex, false, PROBABLE_SCHEDULING_ANALOG_DATE, ProbableDateAnalogWorkflowDetailsInt.class )
                .map(details -> {
                    log.info("ProbableSchedulingAnalogDate is present, need to schedule analog workflow at={}- iun={} id={} ", details.getSchedulingAnalogDate(), iun, recIndex);
                    return details.getSchedulingAnalogDate();
                })
                .orElseGet(() -> {
                    log.info("Courtesy message is not present, analog workflow can be started now  - iun={} id={} ", iun, recIndex);
                    return Instant.now();
                });


        chooseDeliveryUtils.addScheduleAnalogWorkflowToTimeline(recIndex, notification, schedulingDate);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.ANALOG_WORKFLOW);
    }
}
