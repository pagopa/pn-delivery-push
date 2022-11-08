package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class DigitalNotificationSender {
    private final ExternalChannelService externalChannelService;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final SchedulerService schedulerService;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final NextWorkflowActionScheduler nextWorkflowActionScheduler;
    private final InstantNowSupplier instantNowSupplier;
    
    public DigitalNotificationSender(ExternalChannelService externalChannelService,
                                     DigitalWorkFlowUtils digitalWorkFlowUtils,
                                     SchedulerService schedulerService,
                                     PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                     NextWorkflowActionScheduler nextWorkflowActionScheduler, 
                                     InstantNowSupplier instantNowSupplier) {
        this.externalChannelService = externalChannelService;
        this.digitalWorkFlowUtils = digitalWorkFlowUtils;
        this.schedulerService = schedulerService;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.nextWorkflowActionScheduler = nextWorkflowActionScheduler;
        this.instantNowSupplier = instantNowSupplier;
    }

    void checkAddressAndSend(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt addressInfo) {
        String iun = notification.getIun();

        LegalDigitalAddressInt digitalAddress = addressInfo.getDigitalAddress();

        log.debug("CheckAddressAndSend - iun={} id={}", iun, recIndex);

        if (digitalAddress != null) {
            log.info("Address with source={} is available, send notification to external channel - iun={} id={}",
                    addressInfo.getDigitalAddressSource(), iun, recIndex);

            //Se l'indirizzo è disponibile, dunque valorizzato viene inviata la notifica a external channel ...
            digitalWorkFlowUtils.addAvailabilitySourceToTimeline(recIndex, notification, addressInfo.getDigitalAddressSource(), true, addressInfo.getSentAttemptMade());
            sendDigitalNotificationAndScheduleTimeoutAction(notification, digitalAddress, addressInfo, recIndex, false, null);
        } else {
            //... altrimenti si passa alla prossima workflow action
            log.info("Address with source={} is not available, need to start next workflow action - iun={} id={}",
                    addressInfo.getDigitalAddressSource(), iun, recIndex);

            digitalWorkFlowUtils.addAvailabilitySourceToTimeline(recIndex, notification, addressInfo.getDigitalAddressSource(), false, addressInfo.getSentAttemptMade());
            nextWorkflowActionScheduler.scheduleNextWorkflowAction(notification, recIndex, addressInfo, instantNowSupplier.get());
        }
    }
    
    /**
     * Il metodo si occupa di inviare la richiesta a ext-channel
     * e di schedulare una action di timeout (ed eventualmente de-schedulare una precedente action di timeout impostata)
     * NB: La schedulazione del timeout avviene soltanto se l'invio a ext-channel va a buon fine.
     *
     * @param notification notifica
     * @param digitalAddress indirizzo
     * @param addressInfo info
     * @param recIndex id recipient
     * @param sendAlreadyInProgress indica se l'invio è a causa di un retry
     * @param sourceTimelineId eventuale idtimeline che ha dato origine alla richiesta
     */
    void sendDigitalNotificationAndScheduleTimeoutAction(NotificationInt notification,
                                                         LegalDigitalAddressInt digitalAddress,
                                                         DigitalAddressInfoSentAttempt addressInfo,
                                                         Integer recIndex,
                                                         boolean sendAlreadyInProgress,
                                                         String sourceTimelineId){

        String timelineId = externalChannelService.sendDigitalNotification(notification, digitalAddress, addressInfo.getDigitalAddressSource(), recIndex, addressInfo.getSentAttemptMade(), sendAlreadyInProgress);

        digitalWorkFlowUtils.unscheduleTimeoutAction(notification.getIun(), recIndex, sourceTimelineId);

        Duration secondNotificationWorkflowWaitingTime = pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout();
        Instant schedulingDate = Instant.now().plus(secondNotificationWorkflowWaitingTime);

        this.schedulerService.scheduleEvent(notification.getIun(), recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION, timelineId);
        log.info("sendDigitalNotificationAndScheduleTimeoutAction scheduled DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION for iun={} recIdx={} timelineId={} schedulingDate={}", notification.getIun(), recIndex, timelineId, schedulingDate);
    }
}
