package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.completionworkflow.RefinementScheduler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
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
    private final InstantNowSupplier instantNowSupplier;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final PaperChannelUtils paperChannelUtils;
    private final RefinementScheduler refinementScheduler;


    public AnalogWorkflowPaperChannelResponseHandler(NotificationService notificationService,
                                                     PaperChannelService paperChannelService,
                                                     CompletionWorkFlowHandler completionWorkFlow,
                                                     AnalogWorkflowUtils analogWorkflowUtils,
                                                     InstantNowSupplier instantNowSupplier,
                                                     PnDeliveryPushConfigs pnDeliveryPushConfigs,
                                                     AnalogWorkflowHandler analogWorkflowHandler, PaperChannelUtils paperChannelUtils, RefinementScheduler refinementScheduler) {
        this.notificationService = notificationService;
        this.paperChannelService = paperChannelService;
        this.completionWorkFlow = completionWorkFlow;
        this.analogWorkflowUtils = analogWorkflowUtils;
        this.instantNowSupplier = instantNowSupplier;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.analogWorkflowHandler = analogWorkflowHandler;
        this.paperChannelUtils = paperChannelUtils;
        this.refinementScheduler = refinementScheduler;
    }

    public void paperChannelPrepareResponseHandler(PrepareEventInt response) {

        log.info("paperChannelPrepareResponseHandler response iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());

        if (response.getStatusCode().equals("OK"))
        {
            String requestId = response.getRequestId();
            PhysicalAddressInt receiverAddress = response.getReceiverAddress();
            NotificationInt notification = notificationService.getNotificationByIun(response.getIun());
            TimelineElementInternal timelineElementInternal = paperChannelUtils.getPaperChannelNotificationTimelineElement(response.getIun(), response.getRequestId());

            int recIndex = ((RecipientRelatedTimelineElementDetails)timelineElementInternal.getDetails()).getRecIndex();

            // se era una prepare di un analog, procedo con la sendanalog, altrimenti con la send della simpleregistered
            if (timelineElementInternal.getDetails() instanceof SendAnalogDetailsInt){
                int sentAttemptMade = ((SendAnalogDetailsInt)timelineElementInternal.getDetails()).getSentAttemptMade();
                this.paperChannelService.sendAnalogNotification(notification, recIndex, sentAttemptMade, requestId, receiverAddress);
            }
            else if (timelineElementInternal.getDetails() instanceof SimpleRegisteredLetterDetailsInt){
                log.info("paperChannelPrepareResponseHandler prepare response is for simple registered letter, sending it and scheduling refinement iun={} requestId={} statusCode={} statusDesc={} statusDate={}", response.getIun(), response.getRequestId(), response.getStatusCode(), response.getStatusDetail(), response.getStatusDateTime());

                this.paperChannelService.sendSimpleRegisteredLetter(notification, recIndex, requestId, receiverAddress);
                // se l'invio non da errore, vuol dire che la notifica si intende perfezionata
                // La notifica è stata accettata correttamente da paper channel il workflow digitale può considerarsi concluso con successo
                refinementScheduler.scheduleDigitalRefinement(notification, recIndex, instantNowSupplier.get(), EndWorkflowStatus.FAILURE);
            }
            else
                throw new PnInternalException("Unexpected detail of timelineElement timeline=" + requestId, ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED);
        }
        else if (response.getStatusCode().equals("KOUNREACHABLE")) {

            NotificationInt notification = notificationService.getNotificationByIun(response.getIun());
            TimelineElementInternal timelineElementInternal = paperChannelUtils.getPaperChannelNotificationTimelineElement(response.getIun(), response.getRequestId());
            int recIndex = ((RecipientRelatedTimelineElementDetails)timelineElementInternal.getDetails()).getRecIndex();

            this.analogWorkflowHandler.nextWorkflowStep(notification, recIndex, AnalogWorkflowHandler.ATTEMPT_MADE_UNREACHABLE);
        }
    }

    public void paperChannelSendResponseHandler(SendEventInt response) {
        String iun = response.getIun();

        SendAnalogDetailsInt sendPaperDetails = analogWorkflowUtils.getSendAnalogNotificationDetails(response.getIun(), response.getRequestId());

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
                    ).collect(Collectors.toList());
        } else {
            legalFactsListEntryIds = Collections.emptyList();
        }
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_CHECK, "Analog workflow Paper channel response iun={} id={} with status={} and sentAttemptMade={}", iun, recIndex, response.getStatusCode(), sendPaperDetails.getSentAttemptMade())
                .iun(iun)
                .build();
        logEvent.log();
        if (status!= null) {
            switch (status) {
                case OK:
                    // AUD_NT_CHECK
                    logEvent.generateSuccess().log();
                    // La notifica è stata consegnata correttamente da external channel il workflow può considerarsi concluso con successo
                    completionWorkFlow.completionAnalogWorkflow(notification, recIndex, legalFactsListEntryIds, response.getStatusDateTime(), sendPaperDetails.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);
                    break;
                case KO:
                    logEvent.generateFailure("Paper channel analogFailureAttempt with failure case {} ", response.getDeliveryFailureCause()).log();
    
                    // External channel non è riuscito a effettuare la notificazione, si passa al prossimo step del workflow
                    int sentAttemptMade = sendPaperDetails.getSentAttemptMade() + 1;
                    analogWorkflowUtils.addAnalogFailureAttemptToTimeline(notification, sentAttemptMade, legalFactsListEntryIds, response.getDiscoveredAddress(), response.getDeliveryFailureCause() == null ? null : List.of(response.getDeliveryFailureCause()), sendPaperDetails);
                    analogWorkflowHandler.nextWorkflowStep(notification, recIndex, sentAttemptMade);
                    break;
                default:
                    throw new PnInternalException("Invalid status from PaperChannel response", ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
            }
        } else {
            handleStatusProgress(response, iun, recIndex);
        }
    }

    private void handleStatusProgress(SendEventInt event, String iun, Integer recIndex) {
        log.error("Specified response={} is not final  - iun={} id={}", event.getStatusCode(), iun, recIndex);
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
        */
        if (paperStatus == null)
            throw new PnInternalException("Invalid received paper status:" + paperStatus, ERROR_CODE_DELIVERYPUSH_INVALIDRECEIVEDPAPERSTATUS);

        if (this.pnDeliveryPushConfigs.getExternalChannel().getAnalogCodesProgress().contains(paperStatus)) {
            return null;
        }
        if (this.pnDeliveryPushConfigs.getExternalChannel().getAnalogCodesSuccess().contains(paperStatus)) {
            return ResponseStatusInt.OK;
        }
        if (this.pnDeliveryPushConfigs.getExternalChannel().getAnalogCodesFail().contains(paperStatus)) {
            return ResponseStatusInt.KO;
        }

        throw new PnInternalException("Invalid received paper status:" + paperStatus, ERROR_CODE_DELIVERYPUSH_INVALIDRECEIVEDPAPERSTATUS);
    }

}
