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
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PrepareDigitalDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleDigitalWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDATTEMPT;

@Component
@AllArgsConstructor
@Slf4j
public class DigitalWorkFlowHandler {
    //Va ridotta la complessità di questa classe PN-2607
    public static final int MAX_ATTEMPT_NUMBER = 2;

    private final SendAndUnscheduleNotification sendAndUnscheduleNotification;
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final CompletionWorkFlowHandler completionWorkflow;
    private final NationalRegistriesService nationalRegistriesService;
    private final InstantNowSupplier instantNowSupplier;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final DigitalWorkflowFirstSendRepeatHandler digitalWorkflowFirstSendRepeatHandler;
    
    /**
     * Starting digital workflow sending notification information to external channel
     *
     * @param notification   Public Administration notification request
     * @param digitalAddress User address
     * @param addressSource Address source ( PLATFORM, SPECIAL, GENERAL );
     * @param recIndex      User identifier
     */
    public void startDigitalWorkflow(NotificationInt notification, LegalDigitalAddressInt digitalAddress, DigitalAddressSourceInt addressSource, Integer recIndex) {
        log.info("Starting digital workflow - iun={} id={} ", notification.getIun(), recIndex);
        log.info("Address with source={} is available, send notification to external channel - iun={} id={} ", addressSource, notification.getIun(), recIndex);

        final DigitalAddressInfoSentAttempt addressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(addressSource)
                .digitalAddress(digitalAddress)
                .sentAttemptMade(ChooseDeliveryModeUtils.ZERO_SENT_ATTEMPT_NUMBER)
                .lastAttemptDate(Instant.now())
                .build();

        sendAndUnscheduleNotification.sendDigitalNotificationAndScheduleTimeoutAction(
                notification,
                digitalAddress,
                addressInfo,
                recIndex, 
                false,
                null,
                false);
    }

    /**
     * Callback nel caso di schedulazione del secondo ciclo di tentativi
     * @param iun IUN della notifica
     * @param recIndex id recipient
     */
    public void startScheduledNextWorkflow(String iun, Integer recIndex, String timelineId) {
        log.debug("startScheduledNextWorkflow - iun={} recIndex={}", iun, recIndex);

        ScheduleDigitalWorkflowDetailsInt scheduleDigitalWorkflow = digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(iun, timelineId);
        DigitalAddressInfoSentAttempt digitalAddressInfoSentAttempt = getDigitalAddressInfo(scheduleDigitalWorkflow);
        scheduleNextWorkFlowExecuteAction(iun, recIndex, digitalAddressInfoSentAttempt, timelineId);
    }

    /**
     * Schedule Handle digital notification Workflow based on already made attempt
     */
    private void scheduleNextWorkFlowExecuteAction(String iun, Integer recIndex, DigitalAddressInfoSentAttempt lastAttemptMade, String sourceTimelineId) {
        log.info("Schedule Next Digital workflow action - iun={} id={} sourceTimelineId={}", iun, recIndex, sourceTimelineId);

        
        //Viene ottenuta la source del prossimo indirizzo da testare, con il numero di tentativi già effettuati per tale sorgente e la data dell'ultimo tentativo
        DigitalAddressInfoSentAttempt nextAddressInfo = digitalWorkFlowUtils.getNextAddressInfo(iun, recIndex, lastAttemptMade);
        log.debug("Next address source is={} and attempt number already made is={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), nextAddressInfo.getSentAttemptMade(), iun, recIndex);

        // Viene spezzato il flusso perchè le operazione di invio, vanno a salvare in timeline dei record e quindi nel caso vi siano errori di invio, rieseguire
        // il nextaddressinfo potrebbe dare risultati diversi. In questo modo invece, viene calcolato qual è l'indirizzo a cui spedire, e poi si può procedere all'effettivo invio.
        // NB salvo in timeline perchè salvare i dati come actionDetails non era possibile, dato che contenevano dati sensibili
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        String timelineId = digitalWorkFlowUtils.addPrepareSendToTimeline(notification, recIndex, lastAttemptMade, nextAddressInfo, sourceTimelineId);
        schedulerService.scheduleEvent(iun, recIndex, Instant.now(),
                ActionType.DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION, timelineId);
    }

    /**
     * Handle digital notification Workflow based on already made attempt
     */
    public void startNextWorkFlowActionExecute(String iun, Integer recIndex, String timelineId) {
        log.info("Start Next Digital workflow action - iun={} id={} timelineId={}", iun, recIndex, timelineId);

        // recupero le info salvate in precedenza
        PrepareDigitalDetailsInt prepareDigitalDetailsInt = digitalWorkFlowUtils.getPrepareSendDigitalWorkflowTimelineElement(iun, timelineId);
        DigitalAddressInfoSentAttempt lastAttemptMade = digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptLastAttemptMadeFromPrepare(prepareDigitalDetailsInt);
        DigitalAddressInfoSentAttempt nextAddressInfo = digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptNextAddressInfoFromPrepare(prepareDigitalDetailsInt);

        NotificationInt notification = notificationService.getNotificationByIun(iun);


        if (nextAddressInfo.getSentAttemptMade() < MAX_ATTEMPT_NUMBER) {
            switch (nextAddressInfo.getSentAttemptMade()) {
                case 0 -> {
                    log.info("Start check first attempt for source={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), iun, recIndex);
                    checkAndSendNotification(notification, recIndex, nextAddressInfo);
                }
                case 1 -> {
                    log.info("Start second attempt for source={} - iun={} id={}", nextAddressInfo.getDigitalAddressSource(), iun, recIndex);
                    startNextWorkflow7daysAfterLastAttempt(notification, recIndex, nextAddressInfo, lastAttemptMade);
                }
                default -> {
                    log.error("Specified attempt={} is not possibile  - iun={} id={}", nextAddressInfo.getSentAttemptMade(), iun, recIndex);
                    throw new PnInternalException("Specified attempt " + nextAddressInfo.getSentAttemptMade() + " is not possibile", ERROR_CODE_DELIVERYPUSH_INVALIDATTEMPT);
                }
            }
        } else {
            //Sono stati già effettuati tutti i tentativi possibili, la notificazione è quindi fallita
            log.info("All attempts were unsuccessful. Digital workflow is failed, lastAttemptDate={} - iun={} id={}", lastAttemptMade.getLastAttemptDate(), iun, recIndex);
            completionWorkflow.completionDigitalWorkflow(notification, recIndex, lastAttemptMade.getLastAttemptDate(), null, EndWorkflowStatus.FAILURE);
        }
    }

    private void checkAndSendNotificationWithReSend(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt nextAddressInfo) {
        log.debug("checkAndSendNotificationWithReSend  - iun={} id={}", notification.getIun(), recIndex);

        Boolean resendFirstAttempt = false;

        if(isPossibileCaseToRepeat(nextAddressInfo)){
            resendFirstAttempt = digitalWorkflowFirstSendRepeatHandler.handleCheckResend(notification, recIndex, nextAddressInfo);
        }
        
        if(!resendFirstAttempt){
            log.info("No need to resend notification - iun={} id={}", notification.getIun(), recIndex);
            checkAndSendNotification(notification, recIndex, nextAddressInfo);
        }
    }

    private boolean isPossibileCaseToRepeat(DigitalAddressInfoSentAttempt nextAddressInfo) {
        return (DigitalAddressSourceInt.PLATFORM.equals(nextAddressInfo.getDigitalAddressSource()) ||
                DigitalAddressSourceInt.GENERAL.equals(nextAddressInfo.getDigitalAddressSource()))
                &&
                nextAddressInfo.getSentAttemptMade() == 1;
    }

    public void checkAndSendNotification(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt nextAddressInfo) {
        String iun = notification.getIun();
        log.debug("Get notification and recipient completed - iun={} id={}", iun, recIndex);

        if (DigitalAddressSourceInt.GENERAL.equals(nextAddressInfo.getDigitalAddressSource())) {
            log.info("Address is general - iun={} id={}", iun, recIndex);
            nationalRegistriesService.sendRequestForGetDigitalGeneralAddress(
                    notification,
                    recIndex,
                    ContactPhaseInt.SEND_ATTEMPT,
                    nextAddressInfo.getSentAttemptMade(), 
                    nextAddressInfo.getRelatedFeedbackTimelineId()
            );//general address need async call to get it
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

        log.info("Check scheduling nextAttempt, lastAttemptDate={} secondNotificationWorkflowWaitingTime={} schedulingDate={} now={}- iun={} id={}",
                lastAttemptDate, secondNotificationWorkflowWaitingTime, schedulingDate, now, notification.getIun(), recIndex);

        //Vengono aggiunti 7 giorni alla data dell'ultimo tentativo effettuata per questa source
        if (now.isAfter(schedulingDate)) {
            log.info("Next workflow scheduling date={} is passed. Start next workflow - iun={} id={}", schedulingDate, iun, recIndex);
            //Se la data odierna è successiva alla data ottenuta in precedenza, non c'è necessità di schedulare, perchè i 7 giorni necessari di attesa dopo il primo tentativo risultano essere già passati
            checkAndSendNotificationWithReSend(notification, recIndex, nextAddressInfo);
        } else {
            log.info("Next workflow scheduling date={} is not passed. Need to schedule next workflow - iun={} id={}", schedulingDate, iun, recIndex);
            //Se la data è minore alla data odierna, bisogna attendere il completamento dei 7 giorni prima partire con un nuovo workflow per questa source
            String timelineId = digitalWorkFlowUtils.addScheduledDigitalWorkflowToTimeline(notification, recIndex, lastAttemptMade);
            schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_NEXT_ACTION, timelineId, null);
        }
    }

    /**
     * Handle response to request for get special address. If address is present in response, send notification to this address else startNewWorkflow action.
     *
     * @param response Get special address response
     * @param notification      Notification
     * @param prCallDetails     Public registry call details
     */
    public void handleGeneralAddressResponse(NationalRegistriesResponse response, NotificationInt notification, PublicRegistryCallDetailsInt prCallDetails) {
        Integer recIndex = prCallDetails.getRecIndex();
        log.info("Start HandleGeneralAddressResponse for digital workflow - iun={} id={}", notification.getIun(), recIndex);
        
        DigitalAddressInfoSentAttempt lastAttemptAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(response.getDigitalAddress())
                .sentAttemptMade(prCallDetails.getSentAttemptMade())
                .lastAttemptDate(prCallDetails.getSendDate())
                .relatedFeedbackTimelineId(prCallDetails.getRelatedFeedbackTimelineId())
                .build();

        checkAddressAndSend(notification, recIndex, lastAttemptAddressInfo);
    }

    private void checkAddressAndSend(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt addressInfo) {
        String iun = notification.getIun();
        log.info("CheckAddressAndSend for source={} sentAttemptMade={} - iun={} id={}", addressInfo.getDigitalAddressSource(), addressInfo.getSentAttemptMade(), iun, recIndex);

        LegalDigitalAddressInt digitalAddress = addressInfo.getDigitalAddress();
        
        if (digitalAddress != null) {
            log.info("Address with source={} is available, check if  need to send notification to external channel - iun={} id={}",
                    addressInfo.getDigitalAddressSource(), iun, recIndex);
            digitalWorkFlowUtils.addAvailabilitySourceToTimeline(recIndex, notification, addressInfo.getDigitalAddressSource(), true, addressInfo.getSentAttemptMade());

            if(addressInfo.getRelatedFeedbackTimelineId() != null){
                log.info("RelatedFeedbackId is present {}, need to check if is equals to previous attempt - iun={} id={}",
                        addressInfo.getRelatedFeedbackTimelineId(), iun, recIndex);

                //Se tratta del secondo tentativo nel secondo ciclo d'invii a valle del primo tentativo (del secondo ciclo, cioè la ripetizione del primo) andato a buon fine
                //Devo verificare che l'indirizzo ottenuto non sia uguale a quello già utilizzato in quest'ultimo tentativo
                TimelineElementInternal timelineElement = getTimelineElement(iun, recIndex, addressInfo.getRelatedFeedbackTimelineId());
                SendDigitalFeedbackDetailsInt sendDigitalDetailsInt = (SendDigitalFeedbackDetailsInt) timelineElement.getDetails();
                
                if( ! digitalAddress.equals(sendDigitalDetailsInt.getDigitalAddress())){
                    log.info("Found address and previous attempt address are different, notification can proceed to new address found - iun={} id={}",
                             iun, recIndex);

                    //Se l'indirizzo è preso da base dati è diverso da quello appena utilizzato nel tentativo precedente allora effettuo un nuovo invio
                    sendAndUnscheduleNotification.sendDigitalNotificationAndScheduleTimeoutAction(
                            notification,
                            digitalAddress,
                            addressInfo,
                            recIndex,
                            false,
                            null,
                            false);
                }else {
                    log.info("Found address and previous attempt address are equals, notification will be not sent to this address. Check if first attempt was sent successfully - iun={} id={}",
                             iun, recIndex);
                    
                    //Verifico se il primo tentativo è andato a buon fine o devo passare al prossimo step del workflow
                    if(ResponseStatusInt.OK.equals(sendDigitalDetailsInt.getResponseStatus())){
                        //Se il primo tentativo è andato a buon fine il workflow può concludersi con successo

                        log.info("First attempt was sent successfully. Complete workflow with success - iun={} id={}",
                                iun, recIndex);

                        completionWorkflow.completionDigitalWorkflow(
                                notification,
                                recIndex,
                                timelineElement.getTimestamp(),
                                sendDigitalDetailsInt.getDigitalAddress(),
                                EndWorkflowStatus.SUCCESS
                        );
                    }else {
                        log.info("First attempt was not sent successfully. Go to next step - iun={} id={}",
                                iun, recIndex);
                        //altrimenti passo al prossimo step del workflow
                        nextWorkflowStep(notification, recIndex, addressInfo, iun);
                    }
                    
                }
                
            } else {
                //Se non si tratta di una casistica di doppio invio passo ad inviare la notifica normalmente
                sendAndUnscheduleNotification.sendDigitalNotificationAndScheduleTimeoutAction(
                        notification,
                        digitalAddress,
                        addressInfo,
                        recIndex,
                        false,
                        null,
                        false);
            }
            
        } else {
            //Se l'indirizzo recuperato da base dati è nullo
            if(addressInfo.getRelatedFeedbackTimelineId() != null){
                //E si tratta del secondo tentativo nel secondo ciclo d'invii a valle del primo tentativo (del secondo ciclo, cioè la ripetizione del primo) andato a buon fine
                boolean completedWorkflowSuccess = checkFirstAttemptAndCompleteWorkflow(notification, recIndex, addressInfo.getRelatedFeedbackTimelineId(), iun);
                if(! completedWorkflowSuccess){
                    //Se il primo tentativo NON è andato a buon fine si passa al prossimo step del workflow
                    nextWorkflowStep(notification, recIndex, addressInfo, iun);
                }
            } else {
                //Se si tratta di un tentativo classico, passo semplicemente al prossimo step del workflow
                nextWorkflowStep(notification, recIndex, addressInfo, iun);
            }
        }
    }

    boolean checkFirstAttemptAndCompleteWorkflow(NotificationInt notification,
                                                 Integer recIndex,
                                                 String relatedFeedbackTimelineId,
                                                 String iun) {
        TimelineElementInternal timelineElement = getTimelineElement(iun, recIndex, relatedFeedbackTimelineId);
        SendDigitalFeedbackDetailsInt sendDigitalDetailsInt = (SendDigitalFeedbackDetailsInt) timelineElement.getDetails();

        if(ResponseStatusInt.OK.equals(sendDigitalDetailsInt.getResponseStatus())){
            //Se il primo tentativo è andato a buon fine il workflow può concludersi con successo

            completionWorkflow.completionDigitalWorkflow(
                    notification,
                    recIndex,
                    timelineElement.getTimestamp(),
                    sendDigitalDetailsInt.getDigitalAddress(),
                    EndWorkflowStatus.SUCCESS
            );
        }else {
            return false;
        }
            
        return true;
    }

    @NotNull
    private TimelineElementInternal getTimelineElement(String iun, Integer recIndex, String relatedFeedbackTimelineId) {
        Optional<TimelineElementInternal> timelineElementOpt = digitalWorkFlowUtils.getTimelineElement(iun, relatedFeedbackTimelineId);
        TimelineElementInternal timelineElement;

        if(timelineElementOpt.isPresent()){
            timelineElement = timelineElementOpt.get();
        } else {
            log.error("Cannot retrive timelineId={} - iun={} id={}", relatedFeedbackTimelineId, iun, recIndex);
            throw new PnInternalException( "Cannot retrive timelineId", ERROR_CODE_PN_GENERIC_ERROR);
        }
        return timelineElement;
    }

    private void nextWorkflowStep(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt addressInfo, String iun) {
        //... altrimenti si passa alla prossima workflow action
        log.info("Need to start next workflow action - iun={} id={}",
                addressInfo.getDigitalAddressSource(), iun, recIndex);

        digitalWorkFlowUtils.addAvailabilitySourceToTimeline(recIndex, notification, addressInfo.getDigitalAddressSource(), false, addressInfo.getSentAttemptMade());
        scheduleNextWorkFlowExecuteAction(iun, recIndex, addressInfo, null);
    }

    void nextWorkflowStep(DigitalResultInfos digitalResultInfos) {
        //Non è stato possibile effettuare la notificazione, si passa al prossimo step del workflow

        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(digitalResultInfos.getDigitalAddressSourceInt())
                .lastAttemptDate(digitalResultInfos.getTimelineElementInternal().getTimestamp())
                .build();

        scheduleNextWorkFlowExecuteAction(digitalResultInfos.getNotification().getIun(), digitalResultInfos.getRecIndex(), lastAttemptMade, null);
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
        private Boolean isFirstSendRetry;
        private String relatedFeedbackTimelineElement;
    }
}
