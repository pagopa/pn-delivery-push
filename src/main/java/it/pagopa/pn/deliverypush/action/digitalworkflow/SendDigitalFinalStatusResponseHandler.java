package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.details.SendDigitalFinalStatusResponseDetails;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT;

@Component
@AllArgsConstructor
@Slf4j
public class SendDigitalFinalStatusResponseHandler {
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final CompletionWorkFlowHandler completionWorkFlowHandler;
    
    public void handleSendDigitalFinalStatusResponse(String iun, SendDigitalFinalStatusResponseDetails details){
        log.debug("Start handleSendDigitalFinalStatusResponse - iun={}", iun);
        String sendDigitalFeedbackTimelineId = details.getLastAttemptAddressInfo().getRelatedFeedbackTimelineId();
        
        Optional<SendDigitalFeedbackDetailsInt> sendDigitalFeedbackDetailsOpt = timelineService.getTimelineElementDetails(iun, sendDigitalFeedbackTimelineId, SendDigitalFeedbackDetailsInt.class);
        
        if(sendDigitalFeedbackDetailsOpt.isPresent()){
            SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails = sendDigitalFeedbackDetailsOpt.get();

            details = setCorrectDigitalAddressToDetails(details, sendDigitalFeedbackDetails);

            switch (sendDigitalFeedbackDetails.getResponseStatus()) {
                case OK -> handleSuccessfulSending(iun, sendDigitalFeedbackDetails, details);
                case KO -> handleNotSuccessfulSending(iun, sendDigitalFeedbackDetails, details);
                default -> {
                    String msg = String.format("Status %s is not handled - iun=%s",sendDigitalFeedbackDetails.getResponseStatus(), iun);
                    log.error(msg);
                    throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE);
                }
            }
            
        } else {
            String msg = String.format("SendDigitalFeedback %s is not present", sendDigitalFeedbackTimelineId);
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
    }

    private static SendDigitalFinalStatusResponseDetails setCorrectDigitalAddressToDetails(SendDigitalFinalStatusResponseDetails details, SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails) {
        DigitalAddressInfoSentAttempt digitalAddressInfoSentAttemptDetail = details.getLastAttemptAddressInfo()
                .toBuilder()
                .digitalAddress(sendDigitalFeedbackDetails.getDigitalAddress())
                .digitalAddressSource(sendDigitalFeedbackDetails.getDigitalAddressSource())
                .build();

        details = details.toBuilder()
                .lastAttemptAddressInfo(
                        digitalAddressInfoSentAttemptDetail
                ).build();
        return details;
    }

    private void handleSuccessfulSending(String iun, SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails, SendDigitalFinalStatusResponseDetails details) {
        int recIndex = sendDigitalFeedbackDetails.getRecIndex();
        log.debug("handleSuccessfulSending - iun={} id={}", iun, recIndex);
        
        if(details.getIsFirstSendRetry() != null && details.getIsFirstSendRetry()){
            log.info("Is response for firstSendRetry - iun={} id={}",  iun, recIndex);
            //Si tratta della response al primo invio degli eventuali due invii (relativi al secondo ciclo di notifica),
            // c'è da fare un ulteriore tentativo all'indirizzo recuperato da Banca dati se disponibile
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            digitalWorkFlowHandler.checkAndSendNotification(notification, recIndex, details.getLastAttemptAddressInfo());
        } else {
            log.info("Is not response for firstSendRetry - iun={} id={}",  iun, recIndex);

            //Se si tratta di un feedback classico, piuttosto che il feedaback al secondo tentativo del secondo ciclo d'invii per una source, porto la notifica in accettata
            //perchè sono sicuro che almeno questo feedback è positivo, non mi interessa dunque di controllare il primo feedback
            
            NotificationInt notification = notificationService.getNotificationByIun(iun);
            
            //La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
            completionWorkFlowHandler.completionSuccessDigitalWorkflow(
                    notification,
                    recIndex,
                    sendDigitalFeedbackDetails.getNotificationDate(),
                    details.getLastAttemptAddressInfo().getDigitalAddress()
            );
        }
    }

    private void handleNotSuccessfulSending(String iun, SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails, SendDigitalFinalStatusResponseDetails details) {
        int recIndex = sendDigitalFeedbackDetails.getRecIndex();
        log.debug("handleNotSuccessfulSending - iun={} id={}", iun, recIndex);

        if(details.getIsFirstSendRetry() != null && details.getIsFirstSendRetry()){
            log.info("Is response for firstSendRetry - iun={} id={}",  iun, recIndex);

            //Si tratta della response al primo invio degli eventuali due invii (relativi al secondo ciclo di notifica),
            // c'è da fare un ulteriore tentativo all'indirizzo recuperato da Banca dati se disponibile
            DigitalAddressInfoSentAttempt addressInfo = details.getLastAttemptAddressInfo();
            NotificationInt notification = notificationService.getNotificationByIun(iun);

            digitalWorkFlowHandler.checkAndSendNotification(notification, recIndex, addressInfo);
        } else {
            log.info("Is not response for firstSendRetry - iun={} id={}",  iun, recIndex);

            DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                    .digitalAddressSource(details.getLastAttemptAddressInfo().getDigitalAddressSource())
                    .lastAttemptDate(details.getLastAttemptAddressInfo().getLastAttemptDate())
                    .build();

            //devo verificare se si tratta del secondo tentativo relativo al secondo ciclo di notifica per una determinata source
            if(details.getAlreadyPresentRelatedFeedbackTimelineId() != null){
                log.debug("There is a relatedFeedbackTimelineElement={} - iun={} id={}", details.getAlreadyPresentRelatedFeedbackTimelineId(), iun, recIndex);

                NotificationInt notification = notificationService.getNotificationByIun(iun);

                //verifico se il primo tentativo è andato a buon fine ed eventualmente completo il workflow con successo
                boolean completedWorkflowSuccess = digitalWorkFlowHandler.checkFirstAttemptAndCompleteWorkflow(
                        notification, recIndex, details.getAlreadyPresentRelatedFeedbackTimelineId(), iun);
                if(! completedWorkflowSuccess){
                    log.info("Workflow is not completed, need to start next action - iun={} id={}", iun, recIndex );

                    //Se il primo tentativo NON è andato a buon fine si passa al prossimo step del workflow
                    digitalWorkFlowHandler.nextWorkflowStep( iun, recIndex, lastAttemptMade );
                }
            } else {
                //sono nella risposta negativa di un tentativo classico
                digitalWorkFlowHandler.nextWorkflowStep( iun, recIndex, lastAttemptMade );
            }
        }

    }
}
