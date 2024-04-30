package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.F24ResolutionMode;
import it.pagopa.pn.deliverypush.action.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.NotificationChannelType;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import java.time.Instant;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExternalChannelServiceImpl implements ExternalChannelService {
    private final ExternalChannelUtils externalChannelUtils;
    private final ExternalChannelSendClient externalChannel;
    private final NotificationUtils notificationUtils;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final TimelineUtils timelineUtils;
    private final AttachmentUtils attachmentUtils;

    public ExternalChannelServiceImpl(ExternalChannelUtils externalChannelUtils,
                                      ExternalChannelSendClient externalChannel,
                                      NotificationUtils notificationUtils,
                                      DigitalWorkFlowUtils digitalWorkFlowUtils,
                                      NotificationService notificationService, AuditLogService auditLogService,
                                      TimelineUtils timelineUtils, AttachmentUtils attachmentUtils) {
        this.externalChannelUtils = externalChannelUtils;
        this.externalChannel = externalChannel;
        this.notificationUtils = notificationUtils;
        this.digitalWorkFlowUtils = digitalWorkFlowUtils;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.timelineUtils = timelineUtils;
        this.attachmentUtils = attachmentUtils;
    }

    /**
     * Send pec notification to external channel
     * Messaggio con valore legale (PEC)
     * Tramite il sendAlreadyInProgress indica se è il primo tentativo, o se invece è un ritentativo breve
     *  @param notification notitica
     * @param recIndex indice destinatario
     * @param sendAlreadyInProgress indica se l'invio è già stato eseguito e si sta eseguendo un ritentativo
     * @param sendInformation contiene le informazione relative alla send (indirizzo, sorgente, tentativo ...)
     * @return eventId relativo al SEND_DIGITAL_DOMICILE
     */
    @Override
    public String sendDigitalNotification(NotificationInt notification,
                                          Integer recIndex,
                                          boolean sendAlreadyInProgress,
                                          SendInformation sendInformation
    ) {
        log.debug("Start sendDigitalNotification - iun {} id {}", notification.getIun(), recIndex);

        if (timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())){
            log.warn("sendDigitalNotification blocked for cancelled iun {}", notification.getIun());
            return null;
        }
        PnAuditLogEvent logEvent = buildAuditLogEvent(notification.getIun(), sendInformation.getDigitalAddress(), recIndex);

        try {
            DigitalParameters digitalParameters = retrieveDigitalParameters(notification, recIndex, false);

            String eventId;
            if (!sendAlreadyInProgress)
            {
                log.info("Start sendDigitalNotification - iun={} recipientIndex={} attempt={} addressSource={}", notification.getIun(), recIndex, sendInformation.getRetryNumber(), sendInformation.getDigitalAddressSource());

                eventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                        EventId.builder()
                                .iun(notification.getIun())
                                .recIndex(recIndex)
                                .source(sendInformation.getDigitalAddressSource())
                                .sentAttemptMade(sendInformation.getRetryNumber())
                                .isFirstSendRetry(sendInformation.getIsFirstSendRetry())
                                .build()
                );
                externalChannel.sendLegalNotification(notification, digitalParameters.recipientFromIndex, sendInformation.getDigitalAddress(), eventId, digitalParameters.fileKeys, digitalParameters.quickAccessToken);
                
                externalChannelUtils.addSendDigitalNotificationToTimeline(
                        notification,
                        recIndex,
                        sendInformation,
                        eventId
                );
            }
            else
            {
                int progressIndex = digitalWorkFlowUtils.getPreviousTimelineProgress(notification, recIndex, sendInformation.getRetryNumber(), 
                        sendInformation.getIsFirstSendRetry(), sendInformation.getDigitalAddressSource()).size() + 1;

                log.debug("Start sendDigitalNotification for retry - iun={} recipientIndex={} attempt={} progressIndex={}", notification.getIun(), recIndex, sendInformation.getRetryNumber(), progressIndex);

                eventId = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                        EventId.builder()
                                .iun(notification.getIun())
                                .recIndex(recIndex)
                                .source(sendInformation.getDigitalAddressSource())
                                .sentAttemptMade(sendInformation.getRetryNumber())
                                .progressIndex(progressIndex)
                                .isFirstSendRetry(sendInformation.getIsFirstSendRetry())
                                .build()
                );

            externalChannel.sendLegalNotification(
                    notification,
                    digitalParameters.recipientFromIndex,
                    sendInformation.getDigitalAddress(), 
                    eventId,
                    digitalParameters.fileKeys,
                    digitalParameters.quickAccessToken);

                SendInformation digitalAddressFeedback = SendInformation.builder()
                        .retryNumber(sendInformation.getRetryNumber())
                        .eventTimestamp(Instant.now())
                        .digitalAddressSource(sendInformation.getDigitalAddressSource())
                        .digitalAddress(sendInformation.getDigitalAddress())
                        .isFirstSendRetry(sendInformation.getIsFirstSendRetry())
                        .relatedFeedbackTimelineId(sendInformation.getRelatedFeedbackTimelineId())
                        .build();

                digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(
                        notification,
                        EventCodeInt.DP00,
                        recIndex,
                        false,
                        null,
                        digitalAddressFeedback
                );
            }

            logEvent.generateSuccess("successful sent eventId={}", eventId).log();
            return eventId;
        } catch (Exception e) {
            logEvent.generateFailure("Error in sendDigitalNotification, iun={} id={}", notification.getIun(), recIndex, e).log();
            throw e;
        }
    }

    /**
     * Send courtesy message to external channel
     * Messaggio senza valore legale (EMAIL, SMS)
     *
     */
    @Override
    public void sendCourtesyNotification(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId) {
        log.debug("Start sendCourtesyNotification - iun {} id {}", notification.getIun(), recIndex);

        if (timelineUtils.checkIsNotificationCancellationRequested(notification.getIun())){
            log.warn("sendCourtesyNotification blocked for cancelled iun {}", notification.getIun());
            return;
        }
        PnAuditLogEvent logEvent = buildAuditLogEvent(notification.getIun(), courtesyAddress, recIndex, eventId);

        try {
            DigitalParameters digitalParameters = retrieveDigitalParameters(notification, recIndex, true);
            externalChannel.sendCourtesyNotification(notification, notificationUtils.getRecipientFromIndex(notification,recIndex), courtesyAddress, eventId,
                digitalParameters.fileKeys.get(0), //AAR is always the first element
                digitalParameters.quickAccessToken);
            logEvent.generateSuccess().log();
        } catch (Exception e) {
            logEvent.generateFailure("Error in sendCourtesyNotification, iun={} id={}", notification.getIun(), recIndex, e).log();
            throw e;
        }
    }


    private DigitalParameters retrieveDigitalParameters(NotificationInt notification, Integer recIndex, boolean retrieveAarOnly) {
        NotificationRecipientInt recipientFromIndex = notificationUtils.getRecipientFromIndex(notification, recIndex);
        Map<String, String> recipientsQuickAccessLinkTokens = notificationService.getRecipientsQuickAccessLinkToken(notification.getIun());

        List<String> fileKeys = retrieveAarOnly ?
                Collections.singletonList(externalChannelUtils.getAarKey(notification.getIun(), recIndex)) :
                attachmentUtils.retrieveAttachments(notification, recIndex,
                        attachmentUtils.retrieveSendAttachmentMode(notification, NotificationChannelType.DIGITAL_NOTIFICATION),
                        F24ResolutionMode.RESOLVE_WITH_TIMELINE,  Collections.emptyList(), false);

        return new DigitalParameters(fileKeys, recipientFromIndex, recipientsQuickAccessLinkTokens.get(recipientFromIndex.getInternalId()));
    }



    private PnAuditLogEvent buildAuditLogEvent(String iun, LegalDigitalAddressInt legalDigitalAddressInt, int recIndex) {
        if (legalDigitalAddressInt.getType() == LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC) {
            return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_DD_SEND, "sendPECMessage");
        }

        throw new PnInternalException("Unsupported LEGAL_DIGITAL_ADDRESS_TYPE " + legalDigitalAddressInt.getType().getValue(), PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ADDRESSTYPENOTSUPPORTED);
    }

    private PnAuditLogEvent buildAuditLogEvent(String iun, CourtesyDigitalAddressInt courtesyAddress, int recIndex, String eventId) {
        if (courtesyAddress.getType() == CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL) {
            return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_DA_SEND_EMAIL, "sendEmailMessage eventId={}", eventId);
        }
        else
        {
            return auditLogService.buildAuditLogEvent(iun, recIndex, PnAuditLogEventType.AUD_DA_SEND_SMS, "sendSMSMessage eventId={}", eventId);
        }
    }


    private record DigitalParameters(List<String> fileKeys,
                                     NotificationRecipientInt recipientFromIndex,
                                     String quickAccessToken) {}


}
