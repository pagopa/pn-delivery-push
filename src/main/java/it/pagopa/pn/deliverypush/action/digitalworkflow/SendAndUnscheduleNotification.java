package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j
public class SendAndUnscheduleNotification {
    private final ExternalChannelService externalChannelService;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final SchedulerService schedulerService;
    
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
                                                         String sourceTimelineId,
                                                         Boolean isFirstSendRetry){

        SendInformation sendInformation = SendInformation.builder()
                .digitalAddress(digitalAddress)
                .digitalAddressSource(addressInfo.getDigitalAddressSource())
                .retryNumber(addressInfo.getSentAttemptMade())
                .isFirstSendRetry(isFirstSendRetry)
                .relatedFeedbackTimelineId(addressInfo.getRelatedFeedbackTimelineId())
                .build();
        
        String timelineId = externalChannelService.sendDigitalNotification(
                notification,
                recIndex,
                sendAlreadyInProgress,
                sendInformation);

        unscheduleTimeoutAction(notification.getIun(), recIndex, sourceTimelineId);

        Duration digitalNoResponseTimeout = pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout();
        Instant schedulingDate = Instant.now().plus(digitalNoResponseTimeout);

        this.schedulerService.scheduleEvent(notification.getIun(), recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION, timelineId);
        log.info("sendDigitalNotificationAndScheduleTimeoutAction scheduled DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION for iun={} recIdx={} timelineId={} schedulingDate={}", notification.getIun(), recIndex, timelineId, schedulingDate);
    }

    void unscheduleTimeoutAction(String iun, int recIndex, String sourceTimelineId)
    {
        if (sourceTimelineId != null)
        {
            // se trovo un precedente sourceTimelineId, vuol dire che probabilmente sto rischedulando per un ritentativo di invio breve.
            // vado ad de-schedulare l'eventuale action precedentemente schedulata, ma se non la trovo, fa niente, non è un errore!
            this.schedulerService.unscheduleEvent(iun, recIndex, ActionType.DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION, sourceTimelineId);
            log.info("unscheduleTimeoutAction UN-scheduled DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION for iun={} recIdx={} timelineId={} ", iun, recIndex, sourceTimelineId);
        }
    }
}
