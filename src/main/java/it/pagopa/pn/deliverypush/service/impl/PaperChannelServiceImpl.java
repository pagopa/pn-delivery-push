package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.AnalogDtoInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.NotificationChannelType;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PhysicalAddressRelatedTimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import it.pagopa.pn.deliverypush.utils.PaperSendMode;
import it.pagopa.pn.deliverypush.utils.PaperSendModeUtils;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND;

@CustomLog
@AllArgsConstructor
@Service
public class PaperChannelServiceImpl implements PaperChannelService {
    private final PaperChannelUtils paperChannelUtils;
    private final PaperChannelSendClient paperChannelSendClient;
    private final NotificationUtils notificationUtils;
    private final AarUtils aarUtils;
    private final TimelineUtils timelineUtils;
    private final MVPParameterConsumer mvpParameterConsumer;
    private final AnalogWorkflowUtils analogWorkflowUtils;
    private final AuditLogService auditLogService;
    private final PaperSendModeUtils paperSendModeUtils;
    private final AttachmentUtils attachmentUtils;


    /**
     * Send registered letter to external channel
     * to use when all pec send fails
     * Invio di RACCOMANDATA SEMPLICE quando falliscono tutti i tentativi via PEC
     */
    @Override
    public void prepareAnalogNotificationForSimpleRegisteredLetter(NotificationInt notification,  Integer recIndex) {
        log.debug("Start sendNotificationForRegisteredLetter - iun={} recipientIndex={}", notification.getIun(), recIndex);

        if (timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())){
            log.warn("sendNotificationForRegisteredLetter blocked for cancelled iun {}", notification.getIun());
            return;
        }

        boolean isNotificationAlreadyViewed = checkIsNotificationViewedOrPaid(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed){

            prepareSimpleRegisteredLetter(notification, recIndex);

            log.info("Prepare request for registered Letter sent to paperChannel - iun={} id={}", notification.getIun(), recIndex);
        }else {
            log.info("Notification is already viewed or paid, registered Letter will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }


    /**
     * Send paper notification to external channel
     * AR o 890
     *
     */
    @Override
    public void prepareAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        log.debug("Start prepareAnalogNotification - iun {} id {}", notification.getIun(), recIndex);
        if (timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())){
            log.warn("prepareAnalogNotification blocked for cancelled iun {}", notification.getIun());
            return;
        }

        boolean isNotificationAlreadyViewedOrPaid = checkIsNotificationViewedOrPaid(notification.getIun(), recIndex);

        if( !isNotificationAlreadyViewedOrPaid ){
            String senderTaxId = notification.getSender().getPaTaxId();

            if( Boolean.FALSE.equals( mvpParameterConsumer.isMvp( senderTaxId ) ) ){

                prepareAnalogDomicile(notification, recIndex, sentAttemptMade);
                log.info("Paper notification sent to paperChannel - iun={} id={}", notification.getIun(), recIndex);

            }else {
                log.info("Paper message is not handled, paper notification will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
                paperChannelUtils.addPaperNotificationNotHandledToTimeline(notification, recIndex);
            }
        } else {
            log.info("Notification is already viewed or paid, paper notification will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }

    private boolean checkIsNotificationViewedOrPaid(String iun, Integer recIndex) {
        boolean isNotificationAlreadyViewed = timelineUtils.checkIsNotificationViewed(iun, recIndex);
        boolean isNotificationAlreadyPaid = false;

        if ( !isNotificationAlreadyViewed ) {
            isNotificationAlreadyPaid = timelineUtils.checkIsNotificationPaid(iun, recIndex);
            if (isNotificationAlreadyPaid) {
                // è un caso anomalo: la notifica non è stata visualizzata, ma è stata pagata.
                // va segnalato perchè è un caso "strano", non si capisce come abbia fatto l'utente a pagarla senza riceverla
                // cmq, non è il caso di fatal, perchè non serve venga svegliato il repereibile. Quando sarà disponibile
                // un'allarmistica light, questo è un caso da mettere come allarme light
                // TODO mettere come alarm LIGHT
                log.error("Notification is PAID but not VIEWED, should check how! iun={} recIndex={}", iun, recIndex);
            }
        }

        return  isNotificationAlreadyViewed || isNotificationAlreadyPaid;
    }

    private void prepareSimpleRegisteredLetter(NotificationInt notification, Integer recIndex) {
        String eventId = paperChannelUtils.buildPrepareSimpleRegisteredLetterEventId(notification, recIndex);

        // recupero gli allegati
        List<String> attachments = retrieveAttachments(notification, recIndex, retrieveSendAttachmentMode(notification, NotificationChannelType.SIMPLE_REGISTERED_LETTER), true, Collections.emptyList());
        PnAuditLogEvent auditLogEvent = buildAuditLogEvent(notification.getIun(), recIndex, true, eventId, PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER.name(), attachments);

        try {
            // nel caso della simple registgered, l'indirizzo è sempre quello fornito dalla pa
            PhysicalAddressInt receiverAddress = analogWorkflowUtils.getPhysicalAddress(notification, recIndex);

            paperChannelSendClient.prepare (new PaperChannelPrepareRequest(notification,
                    notificationUtils.getRecipientFromIndex(notification, recIndex),
                    receiverAddress, eventId, PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER,
                    attachments, null, null));

            String timelineId = paperChannelUtils.addPrepareSimpleRegisteredLetterToTimeline(notification, receiverAddress, recIndex, eventId);


            auditLogEvent.generateSuccess("Prepare invoked timelineId={}", timelineId).log();
        } catch (Exception exc) {
            auditLogEvent.generateFailure("failed prepare", exc).log();
            throw exc;
        }
    }

    private void prepareAnalogDomicile(NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        String eventId = paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade);

        // recupero gli allegati
        List<String> attachments = retrieveAttachments(notification, recIndex, retrieveSendAttachmentMode(notification, NotificationChannelType.ANALOG_NOTIFICATION), true, Collections.emptyList());
        PhysicalAddressInt.ANALOG_TYPE analogType = getAnalogType(notification);
        PnAuditLogEvent auditLogEvent = buildAuditLogEvent(notification.getIun(), recIndex, true, eventId, analogType.name(), attachments);

        try {
            // nel caso sia un ritentativo, vado in cerca del precedente feedback dell'eventuale discovered address
            String relatedEventId = null;
            PhysicalAddressInt receiverAddress = null;
            PhysicalAddressInt discoveredAddress = null;
            if (sentAttemptMade > 0)
            {
                // ricostruisco il related corrispondente, tanto ha la forma che gli avevo dato io all'iterazione precedente
                // il relatedEventId del primo tentativo serve a paperChannel distinguere e correlare la seconda invocazione dalla prima
                relatedEventId = paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade - 1);

                // ricostruisco il feedback corrispondente, tanto ha la forma che gli avevo dato io all'iterazione precedente.
                // mi serve per recuperare il discoveredAddress all'interno
                String relatedAnalogFeedbackEventId = paperChannelUtils.buildSendAnalogFeedbackEventId(notification, recIndex, sentAttemptMade - 1);

                TimelineElementInternal previousResult = paperChannelUtils.getPaperChannelNotificationTimelineElement(notification.getIun(), relatedAnalogFeedbackEventId);
                discoveredAddress = ((SendAnalogFeedbackDetailsInt)previousResult.getDetails()).getNewAddress();
                if (discoveredAddress != null && !StringUtils.hasText(discoveredAddress.getFullname()))
                {
                    // se il discovered address non contiene il full name, lo imposto alla denominazione del recipient
                    discoveredAddress.setFullname(notification.getRecipients().get(recIndex).getDenomination());
                }

                // se la relatedrequestid non è nulla, il receiver address è quello usato nella prima send
                String eventIdPreviousSend = paperChannelUtils.buildSendAnalogDomicileEventId(notification, recIndex, sentAttemptMade-1);
                TimelineElementInternal previousSendEvent = paperChannelUtils.getPaperChannelNotificationTimelineElement(notification.getIun(), eventIdPreviousSend);

                //PaperChannel ha bisogno per il secondo tentativo dell'indirizzo del primo tentativo
                receiverAddress = ((PhysicalAddressRelatedTimelineElement)previousSendEvent.getDetails()).getPhysicalAddress();
                if (receiverAddress != null && !StringUtils.hasText(receiverAddress.getFullname())) {
                    receiverAddress.setFullname(notification.getRecipients().get(recIndex).getDenomination());
                }
            }
            else
            {
                // se sentAttemptMade è 0, il receiver addres è quello fornito dalla PA
                receiverAddress = analogWorkflowUtils.getPhysicalAddress(notification, recIndex);
            }

            paperChannelSendClient.prepare (new PaperChannelPrepareRequest(notification,
                    notificationUtils.getRecipientFromIndex(notification, recIndex),
                    receiverAddress, eventId, analogType,
                    attachments, relatedEventId, discoveredAddress));


            String timelineId = paperChannelUtils.addPrepareAnalogNotificationToTimeline(notification, receiverAddress, recIndex, relatedEventId, sentAttemptMade, eventId, discoveredAddress);
            auditLogEvent.generateSuccess("Prepare invoked timelineId={}", timelineId).log();
        } catch (Exception exc) {
            auditLogEvent.generateFailure("failed prepare", exc).log();
            throw exc;
        }
    }


    /**
     * Recupera gli allegati della notifica, in base al tipo di invio
     *
     * @param notification notifica
     * @param recIndex indice destinatario
     * @return lista id allegati
     */
    @NotNull
    private List<String> retrieveAttachments(NotificationInt notification, Integer recIndex, SendAttachmentMode sendAttachmentMode, Boolean isPrepareFlow, List<String> replacedF24AttachmentUrls) {
        log.info("retrieveAttachments iun={} recIndex={} sendAttachmentMode={} isPrepareFlow={} replacedF24AttachmentUrls={}", notification.getIun(), recIndex, sendAttachmentMode, isPrepareFlow, replacedF24AttachmentUrls);
        AarGenerationDetailsInt aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);

        List<String> attachments = new ArrayList<>();
        attachments.add(0, aarGenerationDetails.getGeneratedAarUrl());
        // nel caso in cui NON sia simple registered letter, devo allegare anche gli atti

        NotificationRecipientInt notificationRecipientInt = notificationUtils.getRecipientFromIndex(notification, recIndex);

        List<String> res = switch (sendAttachmentMode) {
            case AAR -> attachments;
            case AAR_DOCUMENTS -> {
                attachments.addAll(attachmentUtils.getNotificationAttachments(notification));
                yield attachments;
            }
            case AAR_DOCUMENTS_PAYMENTS -> {
                attachments.addAll(attachmentUtils.getNotificationAttachmentsAndPayments(notification,notificationRecipientInt, recIndex, isPrepareFlow, replacedF24AttachmentUrls));
                yield attachments;
            }
        };
        log.info("retrieveAttachments iun={} recIndex={} attachmentsToSend={}", notification.getIun(), recIndex, res);
        return res;
    }

    @Override
    public String sendSimpleRegisteredLetter(NotificationInt notification, Integer recIndex, String prepareRequestId, PhysicalAddressInt receiverAddress, String productType, List<String> replacedF24AttachmentUrls) {
        log.info("Registered Letter check if send to paperChannel - iun={} id={}", notification.getIun(), recIndex);
        if (timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())){
            log.warn("sendSimpleRegisteredLetter blocked for cancelled iun {}", notification.getIun());
            return null;
        }

        String timelineId = null;
        boolean isNotificationAlreadyViewed = checkIsNotificationViewedOrPaid(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed) {
            log.info("Registered Letter sending to paperChannel - iun={} id={}", notification.getIun(), recIndex);

            // recupero gli allegati
            List<String> attachments = retrieveAttachments(notification, recIndex, retrieveSendAttachmentMode(notification, NotificationChannelType.SIMPLE_REGISTERED_LETTER), false, replacedF24AttachmentUrls);

            PnAuditLogEvent auditLogEvent = buildAuditLogEvent(notification.getIun(), recIndex, false, prepareRequestId, productType, attachments);


            try {
                SendResponse sendResponse = paperChannelSendClient.send(
                        new PaperChannelSendRequest(notification, notificationUtils.getRecipientFromIndex(notification, recIndex),
                                receiverAddress, prepareRequestId, productType, attachments, paperChannelUtils.getSenderAddress(), paperChannelUtils.getSenderAddress()));

                timelineId = paperChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, receiverAddress, recIndex, sendResponse, productType, prepareRequestId, replacedF24AttachmentUrls);
                log.info("Registered Letter sent to paperChannel - iun={} id={}", notification.getIun(), recIndex);
                auditLogEvent.generateSuccess("send success cost={} send timelineId={}", sendResponse.getAmount(), timelineId).log();

            } catch (Exception exc) {
                auditLogEvent.generateFailure("failed send", exc).log();
                throw exc;
            }
        }
        else {
            log.info("Notification is already viewed before send, registered Letter will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }

        return timelineId;
    }

    @Override
    public String sendAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade,
                                         String prepareRequestId, PhysicalAddressInt receiverAddress, String productType, List<String> replacedF24AttachmentUrls) {
        String timelineId = null;

        if (timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())){
            log.warn("sendAnalogNotification blocked for cancelled iun {}", notification.getIun());
            return timelineId;
        }

        boolean isNotificationAlreadyViewed = checkIsNotificationViewedOrPaid(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed) {
            log.info("Analog notification sending to paperChannel - iun={} id={}", notification.getIun(), recIndex);

            // recupero gli allegati
            List<String> attachments = retrieveAttachments(notification, recIndex, retrieveSendAttachmentMode(notification, NotificationChannelType.ANALOG_NOTIFICATION), false, replacedF24AttachmentUrls);

            PnAuditLogEvent auditLogEvent = buildAuditLogEvent(notification.getIun(), recIndex, false, prepareRequestId, productType, attachments);


            try {
                String relatedEventId = null;
                if (sentAttemptMade > 0) {
                    relatedEventId = paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade - 1);
                }

                // IL sender/ar address son impostati a pagopa
                SendResponse sendResponse =  paperChannelSendClient.send(
                        new PaperChannelSendRequest(notification, notificationUtils.getRecipientFromIndex(notification, recIndex),
                                receiverAddress, prepareRequestId, productType, attachments, paperChannelUtils.getSenderAddress(), paperChannelUtils.getSenderAddress()));


                AnalogDtoInt analogDtoInfo = AnalogDtoInt.builder()
                        .sentAttemptMade(sentAttemptMade)
                        .sendResponse(sendResponse)
                        .relatedRequestId(relatedEventId)
                        .productType(productType)
                        .prepareRequestId(prepareRequestId)
                        .build();

                timelineId = paperChannelUtils.addSendAnalogNotificationToTimeline(notification, receiverAddress, recIndex, analogDtoInfo, replacedF24AttachmentUrls);

                log.info("Analog notification sent to paperChannel - iun={} id={}", notification.getIun(), recIndex);
                auditLogEvent.generateSuccess("send success cost={} send timelineId={}", sendResponse.getAmount(), timelineId).log();

            } catch (Exception exc) {
                auditLogEvent.generateFailure("failed send", exc).log();
                throw exc;
            }
        }
        else {
            log.info("Notification is already viewed before send, paper notification will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }

        return timelineId;
    }


    @NotNull
    private PhysicalAddressInt.ANALOG_TYPE getAnalogType(NotificationInt notification) {
        return notification.getPhysicalCommunicationType() == ServiceLevelTypeInt.REGISTERED_LETTER_890 ? PhysicalAddressInt.ANALOG_TYPE.REGISTERED_LETTER_890 : PhysicalAddressInt.ANALOG_TYPE.AR_REGISTERED_LETTER;
    }


    private PnAuditLogEvent buildAuditLogEvent(String iun, int recIndex, boolean isPrepare, String requestId, String analogType, List<String> attachmentsList) {
        String attachments = attachmentsList==null?"":String.join(",", attachmentsList);
        if (isPrepare) {
            return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_PD_PREPARE, "prepareRequest requestId={} analogType={} attachments={}", requestId, analogType, attachments);
        }
        else
        {
            return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_PD_EXECUTE, "sendRequest requestId={} analogType={} attachments={}", requestId, analogType, attachments);
        }
    }

    private SendAttachmentMode retrieveSendAttachmentMode(NotificationInt notification, NotificationChannelType notificationChannelType) {
        PaperSendMode paperSendMode = paperSendModeUtils.getPaperSendMode(notification.getSentAt());
        
        if(paperSendMode != null){
            return switch (notificationChannelType) {
                case ANALOG_NOTIFICATION -> paperSendMode.getAnalogSendAttachmentMode();
                case SIMPLE_REGISTERED_LETTER -> paperSendMode.getSimpleRegisteredLetterSendAttachmentMode();
            };
        }else {
            String msg = String.format("There isn't correct configuration for iun=%s sentAt=%s", notification.getIun(), notification.getSentAt());
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_CONFIGURATION_NOT_FOUND);
        }
    }

}
