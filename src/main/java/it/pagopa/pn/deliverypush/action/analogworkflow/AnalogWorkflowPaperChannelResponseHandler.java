package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.CategorizedAttachmentsResultInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.PrepareEventInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.BaseAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.BaseRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.exceptions.PnPaperChannelChangedCostException;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Component
@AllArgsConstructor
@Slf4j
public class AnalogWorkflowPaperChannelResponseHandler {

    private final NotificationService notificationService;
    private final PaperChannelService paperChannelService;
    private final AnalogWorkflowUtils analogWorkflowUtils;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final PaperChannelUtils paperChannelUtils;
    private final AuditLogService auditLogService;
    private final SchedulerService schedulerService;
    

    public void paperChannelPrepareResponseHandler(PrepareEventInt response) {

        log.info("paperChannelPrepareResponseHandler response iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());


        NotificationInt notification = notificationService.getNotificationByIun(response.getIun());
        TimelineElementInternal timelineElementInternal = paperChannelUtils.getPaperChannelNotificationTimelineElement(response.getIun(), response.getRequestId());

        int recIndex = ((RecipientRelatedTimelineElementDetails)timelineElementInternal.getDetails()).getRecIndex();
        String requestId = response.getRequestId();

        PnAuditLogEvent auditLogEvent = buildPrepareEventAuditLog(response.getIun(), recIndex, response);

        try {
            PrepareEventInt.STATUS_CODE statusCode = PrepareEventInt.STATUS_CODE.valueOf(response.getStatusCode());

            if (statusCode == PrepareEventInt.STATUS_CODE.OK)
            {
                handlerPrepareOK(response, notification, timelineElementInternal, recIndex, requestId, auditLogEvent);
            }
            else if (statusCode == PrepareEventInt.STATUS_CODE.KO) {

                handlePrepareKO(response, notification, timelineElementInternal, recIndex, requestId, auditLogEvent);
            }
        } catch (Exception exc) {
            auditLogEvent.generateFailure("Unexpected error", exc).log();
            throw exc;
        }
    }

    private void handlePrepareKO(PrepareEventInt response, NotificationInt notification, TimelineElementInternal timelineElementInternal, int recIndex, String requestId, PnAuditLogEvent auditLogEvent) {


        // se era una prepare di un analog, procedo con nextworkflow. E' l'unica caso in cui mi interessa gestire il KO (e che può verificarsi da flusso workflo)
        if (timelineElementInternal.getCategory() == TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE){
            // salvo in timeline l'evento di fallimento
            paperChannelUtils.addPrepareAnalogFailureTimelineElement(response.getReceiverAddress(), response.getRequestId(), response.getFailureDetailCode(), recIndex, notification);

            auditLogEvent.generateWarning("Received KO for prepare requestId=" + requestId + " failureDetailCause=" + response.getFailureDetailCode() + " recIndex=" + recIndex).log();
            log.info("paperChannelPrepareResponseHandler prepare response is for analog, setting as unreachable iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());
            this.analogWorkflowHandler.nextWorkflowStep(notification, recIndex, AnalogWorkflowHandler.ATTEMPT_MADE_UNREACHABLE, null);
        }
        else if (timelineElementInternal.getCategory() == TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER){
            log.error("paperChannelPrepareResponseHandler prepare response is for simple registered letter  event is KO and is not expected iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());

            throw new PnInternalException("Unexpected KO for simple registered letter requestId=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
        }
        else
        {
            throw new PnInternalException("Unexpected detail of timelineElement timeline=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
        }
    }

    private void handlerPrepareOK(PrepareEventInt response, NotificationInt notification, TimelineElementInternal timelineElementInternal, int recIndex, String requestId, PnAuditLogEvent auditLogEvent) {
        PhysicalAddressInt receiverAddress = response.getReceiverAddress();
        String productType = response.getProductType();
        List<String> replacedF24AttachmentUrls = response.getReplacedF24AttachmentUrls();
        CategorizedAttachmentsResultInt categorizedAttachmentsResult = response.getCategorizedAttachmentsResult();

        // se era una prepare di un analog, procedo con la sendanalog, altrimenti con la send della simpleregistered
        if (timelineElementInternal.getDetails() instanceof BaseAnalogDetailsInt sendAnalogDetails){
            log.info("paperChannelPrepareResponseHandler prepare response is for analog, sending it iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());
            int sentAttemptMade = sendAnalogDetails.getSentAttemptMade();

            try {
                String timelineId = this.paperChannelService.sendAnalogNotification(notification, recIndex, sentAttemptMade, requestId, receiverAddress, productType, replacedF24AttachmentUrls, categorizedAttachmentsResult);
                String auditlogmessage = timelineId==null?"nothing send":"generated timelineId="+timelineId;
                auditLogEvent.generateSuccess(auditlogmessage).log();
            } catch (PnPaperChannelChangedCostException e) {
                String auditlogmessage = "send cost is different from prepare cost, need to re-do prepare";
                this.paperChannelService.prepareAnalogNotification(notification, recIndex, sentAttemptMade);
                auditLogEvent.generateWarning(auditlogmessage).log();
            }
        }
        else if ( timelineElementInternal.getDetails() instanceof BaseRegisteredLetterDetailsInt ){
            log.info("paperChannelPrepareResponseHandler prepare response is for simple registered letter, now registered letter can be sent iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());

            try {
                String timelineId = this.paperChannelService.sendSimpleRegisteredLetter(notification, recIndex, requestId, receiverAddress, productType, replacedF24AttachmentUrls, categorizedAttachmentsResult);
                String auditlogmessage = timelineId==null?"nothing send":"generated timelineId="+timelineId;
                auditLogEvent.generateSuccess(auditlogmessage).log();
            } catch (PnPaperChannelChangedCostException e) {
                String auditlogmessage = "send cost is different from prepare cost, need to re-do prepare";
                this.paperChannelService.prepareAnalogNotificationForSimpleRegisteredLetter(notification, recIndex);
                auditLogEvent.generateWarning(auditlogmessage).log();
            }
        }
        else
        {
            auditLogEvent.generateFailure("Unexpected detail of timelineElement on OK event timeline=" + requestId).log();
            throw new PnInternalException("Unexpected detail of timelineElement timeline=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
        }
    }

    public void paperChannelSendResponseHandler(SendEventInt response) {
        String iun = response.getIun();
        String requestId = response.getRequestId();

        TimelineElementInternal timelineElementInternal = paperChannelUtils.getPaperChannelNotificationTimelineElement(response.getIun(), response.getRequestId());
        
        if(timelineElementInternal.getDetails() instanceof BaseRegisteredLetterDetailsInt simpleRegisteredLetterDetails){
            handleResponseForSimpleRegisteredLetter(response, simpleRegisteredLetterDetails, iun, timelineElementInternal);
        }
        else if (timelineElementInternal.getDetails() instanceof BaseAnalogDetailsInt sendPaperDetails){
            handleResponseForSendPaper(response, sendPaperDetails, iun, timelineElementInternal);
        } else
            throw new PnInternalException("Unexpected details of timelineElement timeline=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
    }
    
    private void handleResponseForSimpleRegisteredLetter(SendEventInt response, BaseRegisteredLetterDetailsInt simpleRegisteredLetterDetails, String iun, TimelineElementInternal timelineElementInternal) {
        //Al momento l'eventuale risposta alla send della simple registered letter viene solo loggata
        log.info("Received response for SendSimpleRegistered letter, statusCode={} iun={} recIndex={}", 
                response.getStatusCode(), iun, simpleRegisteredLetterDetails.getRecIndex());
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        Integer recIndex = simpleRegisteredLetterDetails.getRecIndex();
        HandleEventUtils.addRecIndexToMdc(recIndex);

        final String prepareRequestId = timelineElementInternal.getElementId();
        String sendRequestId = paperChannelUtils.getSendRequestIdByPrepareRequestId(response.getIun(), prepareRequestId);

        handleStatusProgressSimpleRegisteredLetter(response, simpleRegisteredLetterDetails, notification, response.getAttachments(), sendRequestId);
    }

    private void handleStatusProgressSimpleRegisteredLetter(SendEventInt response,
                                                            BaseRegisteredLetterDetailsInt sendPaperDetails,
                                                            NotificationInt notification,
                                                            List<AttachmentDetailsInt> attachments,
                                                            String sendRequestId) {
        analogWorkflowUtils.addSimpleRegisteredLetterProgressToTimeline(
                notification,
                attachments,
                sendPaperDetails,
                response,
                sendRequestId);
    }

    private void handleResponseForSendPaper(SendEventInt response, BaseAnalogDetailsInt sendPaperDetails, String iun, TimelineElementInternal timelineElementInternal) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        Integer recIndex = sendPaperDetails.getRecIndex();
        HandleEventUtils.addRecIndexToMdc(recIndex);

        ResponseStatusInt status = mapPaperStatusInResponseStatus(response.getStatusCode());

        final String prepareRequestId = timelineElementInternal.getElementId();
        String sendRequestId = paperChannelUtils.getSendRequestIdByPrepareRequestId(response.getIun(), prepareRequestId);

        if (status!= null) {
            switch (status) {
                case PROGRESS ->
                        handleStatusProgress(response, sendPaperDetails, notification, response.getAttachments(), sendRequestId);
                case OK ->
                        handleStatusOK(response, sendPaperDetails, notification, recIndex, response.getAttachments(), sendRequestId);
                case KO ->
                        handleStatusKO(response, sendPaperDetails, notification, recIndex, response.getAttachments(), sendRequestId);
                default ->
                        throw new PnInternalException("Invalid status from PaperChannel response", ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
            }
        } else {
            handleStatusIgnored(response, iun, recIndex);
        }
    }
    
    private void handleStatusProgress(SendEventInt response,
                                      BaseAnalogDetailsInt sendPaperDetails,
                                      NotificationInt notification,
                                      List<AttachmentDetailsInt> attachments,
                                      String sendRequestId) {
        analogWorkflowUtils.addAnalogProgressAttemptToTimeline(
                notification,
                attachments,
                sendPaperDetails,
                response,
                sendRequestId);
    }

    private void handleStatusOK(SendEventInt response, 
                                BaseAnalogDetailsInt sendPaperDetails,
                                NotificationInt notification, 
                                Integer recIndex, 
                                List<AttachmentDetailsInt> attachments,
                                String sendRequestId) {
        PnAuditLogEvent logEvent = buildSendEventAuditLog(notification.getIun(), recIndex, response, sendPaperDetails, attachments);

        // La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
        try {
            String timelineId = analogWorkflowUtils.addAnalogSuccessAttemptToTimeline(notification, attachments,  sendPaperDetails, response, sendRequestId);
            scheduleSendAnalogFinalStatusResponseHandling(notification.getIun(), recIndex, timelineId);
            logEvent.generateSuccess("generated success timelineid={}", timelineId).log();
        } catch (Exception e) {
            logEvent.generateFailure("Error handling execute response", e).log();
            throw  e;
        }
    }

    private void handleStatusKO(SendEventInt response,
                                BaseAnalogDetailsInt sendPaperDetails,
                                NotificationInt notification, 
                                Integer recIndex,
                                List<AttachmentDetailsInt> attachments,
                                String sendRequestId) {
        PnAuditLogEvent logEvent = buildSendEventAuditLog(notification.getIun(), recIndex, response, sendPaperDetails, attachments);

        try {
            // External channel non è riuscito a effettuare la notificazione, si passa al prossimo step del workflow
            String timelineId = analogWorkflowUtils.addAnalogFailureAttemptToTimeline(notification, sendPaperDetails.getSentAttemptMade(), attachments, sendPaperDetails, response, sendRequestId);
            scheduleSendAnalogFinalStatusResponseHandling(notification.getIun(), recIndex, timelineId);
            logEvent.generateWarning("Analog notification failed with failure cause {} generated failure timelineid={}", response.getDeliveryFailureCause(), timelineId).log();

        } catch (Exception e) {
            logEvent.generateFailure("Error handling execute response", e).log();
            throw  e;
        }
    }

    private void scheduleSendAnalogFinalStatusResponseHandling(String iun, int recIndex, String analogFeedbackTimelineId){
        schedulerService.scheduleEvent(
                iun, recIndex, Instant.now(), ActionType.SEND_ANALOG_FINAL_STATUS_RESPONSE, analogFeedbackTimelineId 
        );
    }

    private void handleStatusIgnored(SendEventInt event, String iun, Integer recIndex) {
        log.info("Specified response statusCode={} is not managed  - iun={} id={}", event.getStatusCode(), iun, recIndex);
    }


    private PnAuditLogEvent buildSendEventAuditLog(  String iun, int recIndex, SendEventInt response, BaseAnalogDetailsInt sendPaperDetails, List<AttachmentDetailsInt> attachmentsDetails ){
        String attachments = attachmentsDetails==null?"":attachmentsDetails.stream().map(AttachmentDetailsInt::getUrl).collect(Collectors.joining(","));
        return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_PD_EXECUTE_RECEIVE,
                "Analog workflow Paper channel execute response requestId={} statusCode={} sentAttemptMade={} attachments={} relatedRequestId={}",
                response.getRequestId(), response.getStatusCode(), sendPaperDetails.getSentAttemptMade(), attachments, sendPaperDetails.getRelatedRequestId());
    }

    private PnAuditLogEvent buildPrepareEventAuditLog(  String iun, int recIndex, PrepareEventInt response){

        return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_PD_PREPARE_RECEIVE, "Analog workflow Paper channel prepare response requestId={} statusCode={}", response.getRequestId(), response.getStatusCode());
    }


    private ResponseStatusInt mapPaperStatusInResponseStatus(String paperStatus) {
        /* Codifica sintetica dello stato dell'esito._  <br/>
                - PROGRESS  <br/>
                - OK  <br/>
                - KO  <br/>
        */
        if (paperStatus == null)
            throw new PnInternalException("Invalid received paper status:" + paperStatus, ERROR_CODE_DELIVERYPUSH_INVALIDRECEIVEDPAPERSTATUS);

        try {
            return ResponseStatusInt.valueOf(paperStatus);
        } catch (IllegalArgumentException e) {
            log.info("received paperStatus={} from paper-channel, will be simply skipped because not PROGRESS/OK/KO", paperStatus);
            return null;
        }
    }

}
