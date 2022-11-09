package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelAnalogSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDRECEIVEDPAPERSTATUS;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND;

@Component
@Slf4j
public class AnalogWorkflowHandler {

    private final NotificationService notificationService;
    private final ExternalChannelService externalChannelService;
    private final CompletionWorkFlowHandler completionWorkFlow;
    private final AnalogWorkflowUtils analogWorkflowUtils;
    private final PublicRegistryService publicRegistryService;
    private final InstantNowSupplier instantNowSupplier;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public AnalogWorkflowHandler(NotificationService notificationService,
                                 ExternalChannelService externalChannelService,
                                 CompletionWorkFlowHandler completionWorkFlow,
                                 AnalogWorkflowUtils analogWorkflowUtils,
                                 PublicRegistryService publicRegistryService,
                                 InstantNowSupplier instantNowSupplier,
                                 PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.notificationService = notificationService;
        this.externalChannelService = externalChannelService;
        this.completionWorkFlow = completionWorkFlow;
        this.analogWorkflowUtils = analogWorkflowUtils;
        this.publicRegistryService = publicRegistryService;
        this.instantNowSupplier = instantNowSupplier;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    public void startAnalogWorkflow(String iun, Integer recIndex) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        nextWorkflowStep(notification, recIndex, 0);
    }

    /**
     * Handle analog notification Workflow based on already made attempt
     */
    public void nextWorkflowStep(NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        log.info("Start Analog next workflow action - iun={} id={}", notification.getIun(), recIndex);

        String iun = notification.getIun();
        log.debug("Sent attempt made is={} - iun={} id={}", sentAttemptMade, iun, recIndex);

        switch (sentAttemptMade) {
            case 0:
                log.info("Handle first send attempt - iun={} id={}", iun, recIndex);

                PhysicalAddressInt paProvidedAddress = analogWorkflowUtils.getPhysicalAddress(notification, recIndex);

                if (paProvidedAddress != null) {
                    log.info("Start send notification with Pa address - iun={} id={}", iun, recIndex);
                    //send notification with paAddress
                    externalChannelService.sendAnalogNotification(notification, paProvidedAddress, recIndex, true, sentAttemptMade);
                } else {
                    log.info("Pa address is not available, need to get address from public registry - iun={} id={}", iun, recIndex);
                    //Get address for notification from public registry
                    publicRegistryService.sendRequestForGetPhysicalAddress(notification, recIndex, sentAttemptMade);
                }
                break;
            case 1:
                log.info("Handle second attempt, send request to public registry - iun={} id={}", iun, recIndex);
                //Send attempt was already made, get address from public registry for second send attempt
                publicRegistryService.sendRequestForGetPhysicalAddress(notification, recIndex, sentAttemptMade);
                break;
            case 2:
                // All sent attempts have been made. The user is not reachable
                log.info("User with iun={} and id={} is unreachable, all attempt was failed", iun, recIndex);
                completionWorkFlow.completionAnalogWorkflow(notification, recIndex, null, instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
                break;
            default:
                handleAttemptError(iun, recIndex, sentAttemptMade);
        }
    }

    /**
     * Handle get response for public registry call.
     */
    public void handlePublicRegistryResponse(NotificationInt notification, Integer recIndex, PublicRegistryResponse response, int sentAttemptMade) {
        log.info("Start handlePublicRegistryResponse for analog workflow,sentAttemptMade={} - iun={} id={} ", sentAttemptMade, notification.getIun(), recIndex);

        switch (sentAttemptMade) {
            case 0:
                log.info("Public registry response is for first attempt  - iun={} id={}", notification.getIun(), recIndex);
                checkAddressAndSend(notification, recIndex, response.getPhysicalAddress(), true, sentAttemptMade);
                break;
            case 1:
                log.info("Public registry response is for second attempt  - iun={} id={}", notification.getIun(), recIndex);
                publicRegistrySecondSendResponse(response, notification, recIndex, sentAttemptMade);
                break;
            default:
                handleAttemptError(notification.getIun(), recIndex, sentAttemptMade);
        }
    }

    private void handleAttemptError(String iun, Integer recIndex, int sentAttemptMade) {
        log.error("Specified attempt={} is not possibile  - iun={} id={}", sentAttemptMade, iun, recIndex);
        throw new PnInternalException("Specified attempt " + sentAttemptMade + " is not possibile", PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDATTEMPT);
    }

    private void publicRegistrySecondSendResponse(PublicRegistryResponse response, NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        String iun = notification.getIun();
        log.info("Start publicRegistrySecondSendResponse  - iun={} id={}", iun, recIndex);

        //Vengono ottenute le informazioni del primo invio effettuato tramite external channel dalla timeline
        SendAnalogFeedbackDetailsInt lastSentFeedback = analogWorkflowUtils.getLastTimelineSentFeedback(iun, recIndex);
        log.debug("getLastTimelineSentFeedback completed  - iun={} id={}", iun, recIndex);

        //Se l'indirizzo fornito da public registry è presente ...
        if (response.getPhysicalAddress() != null) {

            PhysicalAddressInt lastUsedAddress = lastSentFeedback.getPhysicalAddress();

            //... e risulta diverso da quello utilizzato nel primo tentativo, viene inviata seconda notifica ad external channel con questo indirizzo
            if (!response.getPhysicalAddress().equals(lastUsedAddress)) {
                log.info("Send second notification to external channel with public registry response address  - iun={} id={}", iun, recIndex);
                externalChannelService.sendAnalogNotification(notification, response.getPhysicalAddress(), recIndex, false, sentAttemptMade);
            } else {
                log.info("First send address and public registry response address are equals  - iun={} id={}", iun, recIndex);
                //... se i due indirizzi sono uguali, viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
                checkInvestigationAddressAndSend(notification, recIndex, sentAttemptMade, lastSentFeedback.getNewAddress());
            }
        } else {
            log.info("Public registry response address is empty  - iun={} id={}", iun, recIndex);
            //Viene verificata la presenza dell'indirizzo ottenuto dall'investigazione del postino
            checkInvestigationAddressAndSend(notification, recIndex, sentAttemptMade, lastSentFeedback.getNewAddress());
        }
    }

    private void checkInvestigationAddressAndSend(NotificationInt notification, Integer recIndex, int sentAttemptMade, PhysicalAddressInt newAddress) {
        log.info("Check address from investigation");
        //Se l'investigation address ricevuto è presente viene effettuato l'invio, tale invio non prevede un ulteriore investigazione (investigation = false)
        checkAddressAndSend(notification, recIndex, newAddress, false, sentAttemptMade);
    }

    private void checkAddressAndSend(NotificationInt notification, Integer recIndex, PhysicalAddressInt address, boolean investigation, int sentAttemptMade) {
        //Se l'indirizzo passato è valorizzato viene inviata la notifica ad externalChannel...
        if (address != null) {
            log.info("Have a valid address, send notification to external channel  - iun={} id={}", notification.getIun(), recIndex);
            externalChannelService.sendAnalogNotification(notification, address, recIndex, investigation, sentAttemptMade);
        } else {
            //... se l'indirizzo non è presente non è possibile raggiungere il destinatario che risulta irreperibile 
            log.info("Address isn't valid, user is unreachable  - iun={} id={}", notification.getIun(), recIndex);
            completionWorkFlow.completionAnalogWorkflow(notification, recIndex, null, instantNowSupplier.get(), null, EndWorkflowStatus.FAILURE);
        }
    }

    public void extChannelResponseHandler(ExtChannelAnalogSentResponseInt response) {
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
                .before(PnAuditLogEventType.AUD_NT_CHECK, "Analog workflow Ext channel response iun={} id={} with status={} and sentAttemptMade={}", iun, recIndex, response.getStatusCode(), sendPaperDetails.getSentAttemptMade())
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
                    logEvent.generateFailure("External channel analogFailureAttempt with failure case {} ", response.getDeliveryFailureCause()).log();
    
                    // External channel non è riuscito a effettuare la notificazione, si passa al prossimo step del workflow
                    int sentAttemptMade = sendPaperDetails.getSentAttemptMade() + 1;
                    analogWorkflowUtils.addAnalogFailureAttemptToTimeline(notification, sentAttemptMade, legalFactsListEntryIds, response.getDiscoveredAddress(), response.getDeliveryFailureCause() == null ? null : List.of(response.getDeliveryFailureCause()), sendPaperDetails);
                    nextWorkflowStep(notification, recIndex, sentAttemptMade);
                    break;
                default:
                    throw new PnInternalException("Invalid status from externalChannel response", ERROR_CODE_DELIVERYPUSH_STATUSNOTFOUND);
            }
        } else {
            handleStatusProgress(response, iun, recIndex);
        }
    }

    private void handleStatusProgress(ExtChannelAnalogSentResponseInt event, String iun, Integer recIndex) {
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
