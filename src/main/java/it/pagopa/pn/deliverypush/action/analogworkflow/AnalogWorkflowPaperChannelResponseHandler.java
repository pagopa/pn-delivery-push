package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.PrepareEventInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.RecipientRelatedTimelineElementDetails;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SimpleRegisteredLetterDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Component
@Slf4j
public class AnalogWorkflowPaperChannelResponseHandler {

    private final NotificationService notificationService;
    private final PaperChannelService paperChannelService;
    private final CompletionWorkFlowHandler completionWorkFlow;
    private final AnalogWorkflowUtils analogWorkflowUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final PaperChannelUtils paperChannelUtils;
    private final AuditLogService auditLogService;


    public AnalogWorkflowPaperChannelResponseHandler(NotificationService notificationService,
                                                     PaperChannelService paperChannelService,
                                                     CompletionWorkFlowHandler completionWorkFlow,
                                                     AnalogWorkflowUtils analogWorkflowUtils,
                                                     PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                                     AnalogWorkflowHandler analogWorkflowHandler,
                                                     PaperChannelUtils paperChannelUtils, AuditLogService auditLogService) {
        this.notificationService = notificationService;
        this.paperChannelService = paperChannelService;
        this.completionWorkFlow = completionWorkFlow;
        this.analogWorkflowUtils = analogWorkflowUtils;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.paperChannelUtils = paperChannelUtils;
        this.auditLogService = auditLogService;
    }

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
            else if (statusCode == PrepareEventInt.STATUS_CODE.KOUNREACHABLE) {

                handlePrepareKOUNREACHABLE(response, notification, timelineElementInternal, recIndex, requestId, auditLogEvent);
            }
        } catch (Exception exc) {
            auditLogEvent.generateFailure("Unexpected error exc=", exc).log();
            throw exc;
        }
    }

    private void handlePrepareKOUNREACHABLE(PrepareEventInt response, NotificationInt notification, TimelineElementInternal timelineElementInternal, int recIndex, String requestId, PnAuditLogEvent auditLogEvent) {
        // se era una prepare di un analog, procedo con nextworkflow
        if (timelineElementInternal.getCategory() == TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE){
            log.info("paperChannelPrepareResponseHandler prepare response is for analog, setting as unreachable iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());
            this.analogWorkflowHandler.nextWorkflowStep(notification, recIndex, AnalogWorkflowHandler.ATTEMPT_MADE_UNREACHABLE, null);
        }
        else if (timelineElementInternal.getCategory() == TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER){
            log.error("paperChannelPrepareResponseHandler prepare response is for simple registered letter  event is KOUNREACHABLE and is not expected iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());

            auditLogEvent.generateFailure("Unexpected KOUNREACHABLE for simple registered letter requestId=" + requestId).log();
            throw new PnInternalException("Unexpected KOUNREACHABLE for simple registered letter requestId=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
        }
        else
        {
            auditLogEvent.generateFailure("Unexpected detail of timelineElement on KOUNREACHABLE event timeline=" + requestId).log();
            throw new PnInternalException("Unexpected detail of timelineElement timeline=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
        }
    }

    private void handlerPrepareOK(PrepareEventInt response, NotificationInt notification, TimelineElementInternal timelineElementInternal, int recIndex, String requestId, PnAuditLogEvent auditLogEvent) {
        PhysicalAddressInt receiverAddress = response.getReceiverAddress();
        String productType = response.getProductType();

        // se era una prepare di un analog, procedo con la sendanalog, altrimenti con la send della simpleregistered
        if (timelineElementInternal.getDetails() instanceof SendAnalogDetailsInt sendAnalogDetails){
            log.info("paperChannelPrepareResponseHandler prepare response is for analog, sending it iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());
            int sentAttemptMade = sendAnalogDetails.getSentAttemptMade();


            String timelineId = this.paperChannelService.sendAnalogNotification(notification, recIndex, sentAttemptMade, requestId, receiverAddress, productType);
            String auditlogmessage = timelineId==null?"nothing sended":"generated timelineId="+timelineId;
            auditLogEvent.generateSuccess(auditlogmessage).log();
        }
        else if ( timelineElementInternal.getDetails() instanceof SimpleRegisteredLetterDetailsInt ){
            log.info("paperChannelPrepareResponseHandler prepare response is for simple registered letter, now registered letter can be sent iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());

            String timelineId = this.paperChannelService.sendSimpleRegisteredLetter(notification, recIndex, requestId, receiverAddress, productType);
            String auditlogmessage = timelineId==null?"nothing sended":"generated timelineId="+timelineId;
            auditLogEvent.generateSuccess(auditlogmessage).log();
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
        
        if(timelineElementInternal.getDetails() instanceof SimpleRegisteredLetterDetailsInt simpleRegisteredLetterDetails){
            //Al momento l'eventuale risposta alla send della simple registered letter viene solo loggata
            log.info("Received response for SendSimpleRegistered letter, statusCode={} iun={} recIndex={}", 
                    response.getStatusCode(), iun, simpleRegisteredLetterDetails.getRecIndex());
        }
        else if (timelineElementInternal.getDetails() instanceof SendAnalogDetailsInt sendPaperDetails){

            NotificationInt notification = notificationService.getNotificationByIun(iun);

            Integer recIndex = sendPaperDetails.getRecIndex();
            ResponseStatusInt status = mapPaperStatusInResponseStatus(response.getStatusCode());
            List<LegalFactsIdInt> legalFactsListEntryIds;
            if (response.getAttachments() != null) {
                legalFactsListEntryIds = response.getAttachments().stream()
                        .map(k -> LegalFactsIdInt.builder()
                                .key(k.getUrl())
                                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                                .build()
                    ).toList();
            } else {
                legalFactsListEntryIds = Collections.emptyList();
            }

            if (status!= null) {
                switch (status) {
                    case PROGRESS -> 
                            handleStatusProgress(response, sendPaperDetails, notification, recIndex, legalFactsListEntryIds);
                    case OK ->
                            handleStatusOK(response, sendPaperDetails, notification, recIndex, legalFactsListEntryIds);
                    case KO -> 
                            handleStatusKO(response, sendPaperDetails, notification, recIndex, legalFactsListEntryIds);
                    default -> 
                            throw new PnInternalException("Invalid status from PaperChannel response", ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
                }
            } else {
                handleStatusIgnored(response, iun, recIndex);
            }
        } else
            throw new PnInternalException("Unexpected details of timelineElement timeline=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
    }

    private void handleStatusProgress(SendEventInt response, SendAnalogDetailsInt sendPaperDetails, NotificationInt notification, Integer recIndex, List<LegalFactsIdInt> legalFactsListEntryIds) {
        analogWorkflowUtils.addAnalogProgressAttemptToTimeline(notification, recIndex, sendPaperDetails.getSentAttemptMade(), legalFactsListEntryIds,  response.getStatusCode(), sendPaperDetails);
    }

    private void handleStatusOK(SendEventInt response, SendAnalogDetailsInt sendPaperDetails, NotificationInt notification, Integer recIndex, List<LegalFactsIdInt> legalFactsListEntryIds) {
        PnAuditLogEvent logEvent = buildSendEventAuditLog(notification.getIun(), recIndex, response, sendPaperDetails, legalFactsListEntryIds);

        // La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
        try {
            String timelineId = analogWorkflowUtils.addAnalogSuccessAttemptToTimeline(notification, sendPaperDetails.getSentAttemptMade(), legalFactsListEntryIds, response.getDiscoveredAddress(), null, sendPaperDetails);
            completionWorkFlow.completionAnalogWorkflow(notification, recIndex, legalFactsListEntryIds, response.getStatusDateTime(), sendPaperDetails.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);

            logEvent.generateSuccess("generated success timelineid={}", timelineId).log();
        } catch (Exception e) {
            logEvent.generateFailure("Error handling execute response ex={}", e).log();
            throw  e;
        }
    }

    private void handleStatusKO(SendEventInt response, SendAnalogDetailsInt sendPaperDetails, NotificationInt notification, Integer recIndex, List<LegalFactsIdInt> legalFactsListEntryIds) {
        PnAuditLogEvent logEvent = buildSendEventAuditLog(notification.getIun(), recIndex, response, sendPaperDetails, legalFactsListEntryIds);

        try {
            // External channel non è riuscito a effettuare la notificazione, si passa al prossimo step del workflow
            String timelineId = analogWorkflowUtils.addAnalogFailureAttemptToTimeline(notification, sendPaperDetails.getSentAttemptMade(), legalFactsListEntryIds, response.getDiscoveredAddress(), response.getDeliveryFailureCause() == null ? null : List.of(response.getDeliveryFailureCause()), sendPaperDetails);
            int sentAttemptMade = sendPaperDetails.getSentAttemptMade() + 1;
            analogWorkflowHandler.nextWorkflowStep(notification, recIndex, sentAttemptMade, response.getStatusDateTime());
            logEvent.generateSuccess("WARNING Analog notification failed with failure cause {} generated failure timelineid={}", response.getDeliveryFailureCause(), timelineId).log();

        } catch (Exception e) {
            logEvent.generateFailure("Error handling execute response ex={}", e).log();
            throw  e;
        }
    }

    private void handleStatusIgnored(SendEventInt event, String iun, Integer recIndex) {
        log.error("Specified response={} is not final  - iun={} id={}", event.getStatusCode(), iun, recIndex);
    }


    private PnAuditLogEvent buildSendEventAuditLog(  String iun, int recIndex, SendEventInt response, SendAnalogDetailsInt sendPaperDetails, List<LegalFactsIdInt> legalFactsListEntryIds ){
        String attachments = legalFactsListEntryIds==null?"":legalFactsListEntryIds.stream().map(LegalFactsIdInt::getKey).collect(Collectors.joining(","));
        return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_PD_EXECUTE_RECEIVE, "Analog workflow Paper channel execute response requestId={} statusCode={} sentAttemptMade={} attachments={} relatedRequestId={}", response.getRequestId(), response.getStatusCode(), sendPaperDetails.getSentAttemptMade(), attachments, sendPaperDetails.getRelatedRequestId());
    }

    private PnAuditLogEvent buildPrepareEventAuditLog(  String iun, int recIndex, PrepareEventInt response){

        return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_PD_PREPARE_RECEIVE, "Analog workflow Paper channel prepare response requestId={} statusCode={}", response.getRequestId(), response.getStatusCode());
    }


    private ResponseStatusInt mapPaperStatusInResponseStatus(String paperStatus) {
        /* Codifica sintetica dello stato dell'esito._  <br/>
                - __001__ Stampato  <br/>
                - __002__ Disponibile al recapitista  <br/>
                - __003__ Preso in carico dal recapitista  <br/>
                - __004__ Consegnata  <br/>
                - __005__ Mancata consegna  <br/>
                - __006__ Furto/Smarrimanto/deterioramento  <br/>
                - __007__ Consegnato Ufficio Postale  <br/>
                - __008__ Mancata consegna Ufficio Postale  <br/>
                - __009__ Compiuta giacenza  <br/>
                - __PC001__ Retry invio  <br/>
        */
        if (paperStatus == null)
            throw new PnInternalException("Invalid received paper status:" + paperStatus, ERROR_CODE_DELIVERYPUSH_INVALIDRECEIVEDPAPERSTATUS);

        if (this.pnDeliveryPushConfigs.getPaperChannel().getAnalogCodesProgress().contains(paperStatus)) {
            return ResponseStatusInt.PROGRESS;
        }
        if (this.pnDeliveryPushConfigs.getPaperChannel().getAnalogCodesSuccess().contains(paperStatus)) {
            return ResponseStatusInt.OK;
        }
        if (this.pnDeliveryPushConfigs.getPaperChannel().getAnalogCodesFail().contains(paperStatus)) {
            return ResponseStatusInt.KO;
        }

        log.info("received eventcode {} from paper-channel, will be simply skipped because not PROGRESS/OK/KO", paperStatus);
        return null;
    }

}
