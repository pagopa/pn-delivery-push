package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleDigitalWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDATTEMPT;

@Component
@AllArgsConstructor
@Slf4j
public class DigitalWorkFlowHandler {
    //FIXME Va ridotta la complessità di questa classe PN-2607
    public static final int MAX_ATTEMPT_NUMBER = 2;

    private final ExternalChannelService externalChannelService;
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final CompletionWorkFlowHandler completionWorkflow;
    private final PublicRegistryService publicRegistryService;
    private final InstantNowSupplier instantNowSupplier;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
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

        sendDigitalNotificationAndScheduleTimeoutAction(notification, digitalAddress, DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(addressSource)
                .digitalAddress(digitalAddress)
                .sentAttemptMade(ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER)
                .lastAttemptDate(Instant.now())
                .build(), recIndex, false, null);
    }

    /**
     * Callback nel caso di schedulazione del secondo ciclo di tentativi
     * @param iun IUN della notifica
     * @param recIndex id recipient
     */
    public void startScheduledNextWorkflow(String iun, Integer recIndex) {
        log.debug("startScheduledNextWorkflow - iun={} recIndex={}", iun, recIndex);

        ScheduleDigitalWorkflowDetailsInt scheduleDigitalWorkflow = digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(iun, recIndex);
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        DigitalAddressInfoSentAttempt digitalAddressInfoSentAttempt = getDigitalAddressInfo(scheduleDigitalWorkflow);
        nextWorkFlowAction(notification, recIndex, digitalAddressInfoSentAttempt);
    }

    /**
     * Handle digital notification Workflow based on already made attempt
     */
    private void nextWorkFlowAction(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt lastAttemptMade) {
        log.info("Start Next Digital workflow action - iun={} id={}", notification.getIun(), recIndex);
        
        String iun = notification.getIun();
        
        //Viene ottenuta la source del prossimo indirizzo da testare, con il numero di tentativi già effettuati per tale sorgente e la data dell'ultimo tentativo
        DigitalAddressInfoSentAttempt nextAddressInfo = digitalWorkFlowUtils.getNextAddressInfo(iun, recIndex, lastAttemptMade);
        log.debug("Next address source is={} and attempt number already made is={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), nextAddressInfo.getSentAttemptMade(), iun, recIndex);

        if (nextAddressInfo.getSentAttemptMade() < MAX_ATTEMPT_NUMBER) {
            switch (nextAddressInfo.getSentAttemptMade()) {
                case 0:
                    log.info("Start check first attempt for source={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), iun, recIndex);
                    checkAndSendNotification(notification, recIndex, nextAddressInfo);
                    break;
                case 1:
                    log.info("Start second attempt for source={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), iun, recIndex);
                    startNextWorkflow7daysAfterLastAttempt(notification, recIndex, nextAddressInfo, lastAttemptMade);
                    break;
                default:
                    log.error("Specified attempt={} is not possibile  - iun={} id={}", nextAddressInfo.getSentAttemptMade(), iun, recIndex);
                    throw new PnInternalException("Specified attempt " + nextAddressInfo.getSentAttemptMade() + " is not possibile", ERROR_CODE_DELIVERYPUSH_INVALIDATTEMPT);
            }
        } else {
            //Sono stati già effettuati tutti i tentativi possibili, la notificazione è quindi fallita
            log.info("All attempts were unsuccessful. Digital workflow is failed, lastAttemptDate={} - iun={} id={}", lastAttemptMade.getLastAttemptDate(), iun, recIndex);
            completionWorkflow.completionDigitalWorkflow(notification, recIndex, lastAttemptMade.getLastAttemptDate(), null, EndWorkflowStatus.FAILURE);
        }
    }

    private void checkAndSendNotification(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt nextAddressInfo) {
        log.debug("CheckAndSendNotification  - iun={} id={}", notification.getIun(), recIndex);
        
        String iun = notification.getIun();
        log.debug("Get notification and recipient completed - iun={} id={}", iun, recIndex);

        if (DigitalAddressSourceInt.GENERAL.equals(nextAddressInfo.getDigitalAddressSource())) {
            log.info("Address is general - iun={} id={}", iun, recIndex);
            publicRegistryService.sendRequestForGetDigitalGeneralAddress(notification, recIndex, ContactPhaseInt.SEND_ATTEMPT, nextAddressInfo.getSentAttemptMade());//general address need async call to get it
        } else {
            log.debug("Address source is not general - iun={} id={}", iun, recIndex);

            //Viene ottenuto l'indirizzo a partire dalla source
            LegalDigitalAddressInt destinationAddress = digitalWorkFlowUtils.getAddressFromSource(nextAddressInfo.getDigitalAddressSource(), recIndex, notification);
            nextAddressInfo = nextAddressInfo.toBuilder().digitalAddress(destinationAddress).build();
            log.debug("Get address completed - iun={} id={}", iun, recIndex);
            //Viene Effettuato il check dell'indirizzo e l'eventuale send
            checkAddressAndSend(notification, recIndex, nextAddressInfo);
        }
    }

    /**
     * If for this address source 7 days has already passed since the last made attempt, for example because have already performed scheduling for previously
     * tried address, the notification step is called, else it is scheduled.
     */
    private void startNextWorkflow7daysAfterLastAttempt(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt nextAddressInfo, DigitalAddressInfoSentAttempt lastAttemptMade) {
        log.debug("StartNextWorkflow7daysAfterLastAttempt - iun={} id={}", notification.getIun(), recIndex);
        
        String iun = notification.getIun();
        Instant lastAttemptDate = nextAddressInfo.getLastAttemptDate();

        Duration secondNotificationWorkflowWaitingTime = pnDeliveryPushConfigs.getTimeParams().getSecondNotificationWorkflowWaitingTime();
        
        Instant schedulingDate = lastAttemptDate.plus(secondNotificationWorkflowWaitingTime);
        Instant now = instantNowSupplier.get();

        log.debug("Check scheduling nextAttempt, lastAttemptDate={} secondNotificationWorkflowWaitingTime={} schedulingDate={} now={}- iun={} id={}",
                lastAttemptDate, secondNotificationWorkflowWaitingTime, schedulingDate, now, notification.getIun(), recIndex);

        //Vengono aggiunti 7 giorni alla data dell'ultimo tentativo effettuata per questa source
        if (now.isAfter(schedulingDate)) {
            log.info("Next workflow scheduling date={} is passed. Start next workflow - iun={} id={}", schedulingDate, iun, recIndex);
            //Se la data odierna è successiva alla data ottenuta in precedenza, non c'è necessità di schedulare, perchè i 7 giorni necessari di attesa dopo il primo tentativo risultano essere già passati
            checkAndSendNotification(notification, recIndex, nextAddressInfo);
        } else {
            log.info("Next workflow scheduling date={} is not passed. Need to schedule next workflow - iun={} id={}", schedulingDate, iun, recIndex);
            //Se la data è minore alla data odierna, bisogna attendere il completamento dei 7 giorni prima partire con un nuovo workflow per questa source
            digitalWorkFlowUtils.addScheduledDigitalWorkflowToTimeline(notification, recIndex, lastAttemptMade);
            schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_NEXT_ACTION);
        }
    }

    /**
     * Handle response to request for get special address. If address is present in response, send notification to this address else startNewWorkflow action.
     *
     * @param response Get special address response
     * @param notification      Notification
     * @param prCallDetails     Public registry call details
     */
    public void handleGeneralAddressResponse(PublicRegistryResponse response, NotificationInt notification, PublicRegistryCallDetailsInt prCallDetails) {
        Integer recIndex = prCallDetails.getRecIndex();
        log.info("Start HandleGeneralAddressResponse for digital workflow - iun={} id={}", notification.getIun(), recIndex);
        
        DigitalAddressInfoSentAttempt lastAttemptAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(response.getDigitalAddress())
                .sentAttemptMade(prCallDetails.getSentAttemptMade())
                .lastAttemptDate(prCallDetails.getSendDate())
                .build();

        checkAddressAndSend(notification, recIndex, lastAttemptAddressInfo);
    }

    private void checkAddressAndSend(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt addressInfo) {
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
            nextWorkFlowAction(notification, recIndex, addressInfo);
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

        unscheduleTimeoutAction(notification.getIun(), recIndex, sourceTimelineId);

        Duration secondNotificationWorkflowWaitingTime = pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout();
        Instant schedulingDate = Instant.now().plus(secondNotificationWorkflowWaitingTime);

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


    void nextWorkflowStep(DigitalResultInfos digitalResultInfos) {
        //Non è stato possibile effettuare la notificazione, si passa al prossimo step del workflow

        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(digitalResultInfos.getDigitalAddressSourceInt())
                .lastAttemptDate(digitalResultInfos.getTimelineElementInternal().getTimestamp())
                .build();

        nextWorkFlowAction(digitalResultInfos.getNotification(), digitalResultInfos.getRecIndex(), lastAttemptMade);
    }

    private DigitalAddressInfoSentAttempt getDigitalAddressInfo(ScheduleDigitalWorkflowDetailsInt scheduleDigitalWorkflow) {
        return DigitalAddressInfoSentAttempt.builder()
                .digitalAddress(scheduleDigitalWorkflow.getDigitalAddress())
                .digitalAddressSource(scheduleDigitalWorkflow.getDigitalAddressSource())
                .lastAttemptDate(scheduleDigitalWorkflow.getLastAttemptDate())
                .sentAttemptMade(scheduleDigitalWorkflow.getSentAttemptMade())
                .build();
    }

    @Data
    protected static class DigitalResultInfos{
        private int recIndex;
        private int retryNumber;
        private LegalDigitalAddressInt digitalAddressInt;
        private DigitalAddressSourceInt digitalAddressSourceInt;
        private ExtChannelDigitalSentResponseInt response;
        private NotificationInt notification;
        private ResponseStatusInt status;
        private TimelineElementInternal timelineElementInternal;
    }
}
