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
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

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


    public AnalogWorkflowPaperChannelResponseHandler(NotificationService notificationService,
                                                     PaperChannelService paperChannelService,
                                                     CompletionWorkFlowHandler completionWorkFlow,
                                                     AnalogWorkflowUtils analogWorkflowUtils,
                                                     PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                                     AnalogWorkflowHandler analogWorkflowHandler, 
                                                     PaperChannelUtils paperChannelUtils) {
        this.notificationService = notificationService;
        this.paperChannelService = paperChannelService;
        this.completionWorkFlow = completionWorkFlow;
        this.analogWorkflowUtils = analogWorkflowUtils;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.paperChannelUtils = paperChannelUtils;
    }

    public void paperChannelPrepareResponseHandler(PrepareEventInt response) {

        log.info("paperChannelPrepareResponseHandler response iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());

        NotificationInt notification = notificationService.getNotificationByIun(response.getIun());
        TimelineElementInternal timelineElementInternal = paperChannelUtils.getPaperChannelNotificationTimelineElement(response.getIun(), response.getRequestId());

        int recIndex = ((RecipientRelatedTimelineElementDetails)timelineElementInternal.getDetails()).getRecIndex();
        String requestId = response.getRequestId();

        PrepareEventInt.STATUS_CODE statusCode = PrepareEventInt.STATUS_CODE.valueOf(response.getStatusCode());

        if (statusCode == PrepareEventInt.STATUS_CODE.OK)
        {
            PhysicalAddressInt receiverAddress = response.getReceiverAddress();
            String productType = response.getProductType();

            // se era una prepare di un analog, procedo con la sendanalog, altrimenti con la send della simpleregistered
            if (timelineElementInternal.getDetails() instanceof SendAnalogDetailsInt sendAnalogDetails){
                log.info("paperChannelPrepareResponseHandler prepare response is for analog, sending it iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());
                int sentAttemptMade = sendAnalogDetails.getSentAttemptMade();
                this.paperChannelService.sendAnalogNotification(notification, recIndex, sentAttemptMade, requestId, receiverAddress, productType);
            }
            else if ( timelineElementInternal.getDetails() instanceof SimpleRegisteredLetterDetailsInt ){
                log.info("paperChannelPrepareResponseHandler prepare response is for simple registered letter, now registered letter can be sent iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());

                this.paperChannelService.sendSimpleRegisteredLetter(notification, recIndex, requestId, receiverAddress, productType);
            }
            else
                throw new PnInternalException("Unexpected detail of timelineElement timeline=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
        }
        else if (statusCode == PrepareEventInt.STATUS_CODE.KOUNREACHABLE) {

            // se era una prepare di un analog, procedo con la sendanalog, altrimenti con la send della simpleregistered
            if (timelineElementInternal.getCategory() == TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE){
                log.info("paperChannelPrepareResponseHandler prepare response is for analog, setting as unreachable iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());
                this.analogWorkflowHandler.nextWorkflowStep(notification, recIndex, AnalogWorkflowHandler.ATTEMPT_MADE_UNREACHABLE, null);
            }
            else if (timelineElementInternal.getCategory() == TimelineElementCategoryInt.PREPARE_SIMPLE_REGISTERED_LETTER){
                log.error("paperChannelPrepareResponseHandler prepare response is for simple registered letter  event is KOUNREACHABLE and is not expected iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());

                throw new PnInternalException("Unexpected KOUNREACHABLE for simple registered letter requestId=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
            }
            else
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
        // AUD_NT_CHECK
        PnAuditLogEvent logEvent = buildAuditLog(notification.getIun(), recIndex, response, sendPaperDetails);
        logEvent.generateSuccess().log();
        // La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
        analogWorkflowUtils.addAnalogSuccessAttemptToTimeline(notification, sendPaperDetails.getSentAttemptMade(), legalFactsListEntryIds, response.getDiscoveredAddress(), null, sendPaperDetails);
        completionWorkFlow.completionAnalogWorkflow(notification, recIndex, legalFactsListEntryIds, response.getStatusDateTime(), sendPaperDetails.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);
    }

    private void handleStatusKO(SendEventInt response, SendAnalogDetailsInt sendPaperDetails, NotificationInt notification, Integer recIndex, List<LegalFactsIdInt> legalFactsListEntryIds) {
        PnAuditLogEvent logEvent = buildAuditLog(notification.getIun(), recIndex, response, sendPaperDetails);
        logEvent.generateSuccess("WARNING Analog notification failed with failure cause {} ", response.getDeliveryFailureCause()).log();

        // External channel non è riuscito a effettuare la notificazione, si passa al prossimo step del workflow
        analogWorkflowUtils.addAnalogFailureAttemptToTimeline(notification, sendPaperDetails.getSentAttemptMade(), legalFactsListEntryIds, response.getDiscoveredAddress(), response.getDeliveryFailureCause() == null ? null : List.of(response.getDeliveryFailureCause()), sendPaperDetails);
        int sentAttemptMade = sendPaperDetails.getSentAttemptMade() + 1;
        analogWorkflowHandler.nextWorkflowStep(notification, recIndex, sentAttemptMade, response.getStatusDateTime());
    }

    private void handleStatusIgnored(SendEventInt event, String iun, Integer recIndex) {
        log.error("Specified response={} is not final  - iun={} id={}", event.getStatusCode(), iun, recIndex);
    }


    private PnAuditLogEvent buildAuditLog(  String iun, int recIndex, SendEventInt response, SendAnalogDetailsInt sendPaperDetails ){
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();

        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_CHECK, "Analog workflow Paper channel response iun={} id={} with status={} and sentAttemptMade={}", iun, recIndex, response.getStatusCode(), sendPaperDetails.getSentAttemptMade())
                .iun(iun)
                .build();
        logEvent.log();
        return logEvent;
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
