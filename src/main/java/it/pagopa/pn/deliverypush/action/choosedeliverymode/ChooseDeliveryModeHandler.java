package it.pagopa.pn.deliverypush.action.choosedeliverymode;

import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ProbableDateAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import it.pagopa.pn.deliverypush.service.NotificationService;
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
            getGeneralAddress(recIndex, notification);
        } else {
            pfStartOldWorkflow(recIndex, notification);
        }
        log.info("END chooseDeliveryTypeAndStartWorkflow process - iun={} recipientIndex={}", notification.getIun(), recIndex);
    }

    private void pfStartOldWorkflow(Integer recIndex, NotificationInt notification) {
        //Verifico presenza indirizzo di piattaforma, ...
        Optional<LegalDigitalAddressInt> platformAddressOpt = chooseDeliveryUtils.retrievePlatformAddress(notification, recIndex);
        // ... se non lo trovo, verifico presenza indirizzo speciale, ...
        if (platformAddressOpt.isEmpty()) {
            LegalDigitalAddressInt specialAddress = chooseDeliveryUtils.retrieveSpecialAddress(notification, recIndex);
            if (specialAddress == null || !StringUtils.hasText(specialAddress.getAddress())) {
                // ... se non lo trovo, verifico presenza indirizzo generale
                getGeneralAddress(recIndex, notification);
            }else{
                digitalWorkFlowHandler.startDigitalWorkflow(notification, specialAddress, DigitalAddressSourceInt.SPECIAL, recIndex);
            }
        }else{
            digitalWorkFlowHandler.startDigitalWorkflow(notification, platformAddressOpt.get(), DigitalAddressSourceInt.PLATFORM, recIndex);
        }
    }

    private void checkSpecialAndPlatformAddress(NotificationInt notification, Integer recIndex) {
        //Verifico presenza indirizzo speciale, ...
        LegalDigitalAddressInt specialAddress = chooseDeliveryUtils.retrieveSpecialAddress(notification, recIndex);
        // ... se non lo trovo, verifico presenza indirizzo di piattaforma, ...
        if (specialAddress == null) {
            Optional<LegalDigitalAddressInt> platformAddressOpt = chooseDeliveryUtils.retrievePlatformAddress(notification, recIndex);
            // ... se non lo trovo, parte il flusso di invio notifica analogica.
            if (platformAddressOpt.isEmpty()) {
                scheduleAnalogWorkflow(notification, recIndex);
            }else{
                digitalWorkFlowHandler.startDigitalWorkflow(notification, platformAddressOpt.get(), DigitalAddressSourceInt.PLATFORM, recIndex);
            }
        }
        else {
            digitalWorkFlowHandler.startDigitalWorkflow(notification, specialAddress, DigitalAddressSourceInt.SPECIAL, recIndex);
        }
    }

    private void getGeneralAddress(Integer recIndex, NotificationInt notification) {
        nationalRegistriesService.sendRequestForGetDigitalGeneralAddress(notification, recIndex, ContactPhaseInt.CHOOSE_DELIVERY, ChooseDeliveryModeUtilsImpl.ZERO_SENT_ATTEMPT_NUMBER, null);
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
                checkSpecialAndPlatformAddress(notification, recIndex);
            } else {
                scheduleAnalogWorkflow(notification, recIndex);
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
