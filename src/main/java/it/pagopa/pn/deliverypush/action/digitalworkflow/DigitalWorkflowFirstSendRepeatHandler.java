package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class DigitalWorkflowFirstSendRepeatHandler {
    private final TimelineService timelineService;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final SendAndUnscheduleNotification sendAndUnscheduleNotification;

    public boolean handleCheckResend(NotificationInt notification , int recIndex, DigitalAddressInfoSentAttempt nextAddressInfo){
        log.info("handleCheckResend source={} sentAttemptMade={} - iun={} recIndex={} ", nextAddressInfo.getDigitalAddressSource(),
                nextAddressInfo.getSentAttemptMade(), notification.getIun(), recIndex);
        
        String firstSendDigitalDomicileEventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(nextAddressInfo.getDigitalAddressSource())
                        .sentAttemptMade(0)
                        .isFirstSendRetry(Boolean.FALSE)
                        .build());

        Optional<SendDigitalDetailsInt> sendDigitalDetailsOpt =  timelineService.getTimelineElementDetails(notification.getIun(), firstSendDigitalDomicileEventId, SendDigitalDetailsInt.class);
        
        if (sendDigitalDetailsOpt.isPresent()){
            log.info("there is a first attempt for the source={} - iun={} recIndex={} ", nextAddressInfo.getDigitalAddressSource(), notification.getIun(), recIndex);

            //E' stato effettuato un primo tentativo per questa source, si procede ad effettuare un ulteriore tentativo per lo stesso indirizzo
            SendDigitalDetailsInt sendDigitalDetailsInt = sendDigitalDetailsOpt.get();
            
            //Viene verificato se il ritentativo è stato già effettuato
            //TODO Verificare se necessario effettuare quest ulteriore check
            Optional<SendDigitalDetailsInt> firstSendRetryOptElement = getFirstSendRetryOptElement(notification, recIndex, nextAddressInfo);

            if (firstSendRetryOptElement.isEmpty()){
                log.info("Resend notification for source={} - iun={} recIndex={} ", nextAddressInfo.getDigitalAddressSource(), notification.getIun(), recIndex);

                //Se non è stato effettuato il ritentativo procedo a farlo
                sendAndUnscheduleNotification.sendDigitalNotificationAndScheduleTimeoutAction(
                        notification,
                        sendDigitalDetailsInt.getDigitalAddress(),
                        nextAddressInfo,
                        recIndex,
                        false,
                        null,
                        true
                );
                return true;
            }else {
                log.warn("Resend notification is already sent for source={} - iun={} recIndex={} ", nextAddressInfo.getDigitalAddressSource(), notification.getIun(), recIndex);
            }

        } else {
            log.info("There isn't a first attempt for the source={} - iun={} recIndex={} ", nextAddressInfo.getDigitalAddressSource(), notification.getIun(), recIndex);
        }

        log.info("Notification not re-sent for source={} - iun={} recIndex={} ", nextAddressInfo.getDigitalAddressSource(), notification.getIun(), recIndex);

        return false;
    }

    private Optional<SendDigitalDetailsInt> getFirstSendRetryOptElement(NotificationInt notification, int recIndex, DigitalAddressInfoSentAttempt nextAddressInfo) {
        String firstSendRetry = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(nextAddressInfo.getDigitalAddressSource())
                        .sentAttemptMade(nextAddressInfo.getSentAttemptMade())
                        .isFirstSendRetry(Boolean.TRUE)
                        .build());

        return  timelineService.getTimelineElementDetails(notification.getIun(), firstSendRetry, SendDigitalDetailsInt.class);
    }
}
