package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.legalfacts.PdfInfo;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SAVENOTIFICATIONFAILED;
import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;

@Slf4j
@Service
public class SaveLegalFactsServiceImpl implements SaveLegalFactsService {

    private static final String SAVE_LEGAL_FACT_EXCEPTION_MESSAGE = "Generating %s legal fact for IUN=%s and recipientId=%s";
    public static final String LEGALFACTS_MEDIATYPE_STRING = "application/pdf";
    public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
    public static final String SAVED = "SAVED";
    public static final String PN_AAR = "PN_AAR";

    private final LegalFactGenerator legalFactBuilder;

    private final SafeStorageService safeStorageService;

    public SaveLegalFactsServiceImpl(LegalFactGenerator legalFactBuilder,
                                     SafeStorageService safeStorageService) {
        this.legalFactBuilder = legalFactBuilder;
        this.safeStorageService = safeStorageService;
    }

    public Mono<String> saveLegalFact(byte[] legalFact) {
        FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
        fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
        fileCreationRequest.setDocumentType(PN_LEGAL_FACTS);
        fileCreationRequest.setStatus(SAVED);
        fileCreationRequest.setContent(legalFact);
        
        return safeStorageService.createAndUploadContent(fileCreationRequest)
                .map( fileCreationResponse -> SAFE_STORAGE_URL_PREFIX + fileCreationResponse.getKey());
    }

    public PdfInfo saveAAR(NotificationInt notification,
                           NotificationRecipientInt recipient,
                           String quickAccessToken) {
        try {
            log.debug("Start Save AAR - iun={}", notification.getIun());

            byte[] pdfByte = legalFactBuilder.generateNotificationAAR(notification, recipient, quickAccessToken);
            int numberOfPages = legalFactBuilder.getNumberOfPages(pdfByte);

            FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
            fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
            fileCreationRequest.setDocumentType(PN_AAR);
            fileCreationRequest.setStatus(SAVED);
            fileCreationRequest.setContent(pdfByte);
            
            return safeStorageService.createAndUploadContent(fileCreationRequest).map( fileCreationResponse ->{
                        PdfInfo pdfInfo = PdfInfo.builder()
                                .key(SAFE_STORAGE_URL_PREFIX + fileCreationResponse.getKey())
                                .numberOfPages(numberOfPages)
                                .build();

                        log.debug("End Save AAR - iun={}", notification.getIun());
                        
                        return pdfInfo;
                    }
            ).block();

        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "AAR", notification.getIun(), "N/A");
            log.error("Exception in saveAAR ex=", exc);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }
    }

    public String saveNotificationReceivedLegalFact(NotificationInt notification) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "SaveNotificationReceivedLegalFact - iun={}", notification.getIun())
                .iun(notification.getIun())
                .build();
        logEvent.log();
        try {
            log.debug("Start saveNotificationReceivedLegalFact - iun={}", notification.getIun());
            
            return this.saveLegalFact(legalFactBuilder.generateNotificationReceivedLegalFact(notification))
                    .map( responseUrl -> {
                        log.debug("End saveNotificationReceivedLegalFact - iun={}", notification.getIun());
                        logEvent.generateSuccess("SaveNotificationReceivedLegalFact success with fileKey={} - iun={}", responseUrl, notification.getIun()).log();
                        return responseUrl;
                    }).block();
            
        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "REQUEST_ACCEPTED", notification.getIun(), "N/A");
            logEvent.generateFailure("Exception in saveNotificationReceivedLegalFact ex={}", exc).log();
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }

    }

    public String savePecDeliveryWorkflowLegalFact(
            List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel,
            NotificationInt notification,
            NotificationRecipientInt recipient,
            EndWorkflowStatus status,
            Instant completionWorkflowDate
    ) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "SavePecDeliveryWorkflowLegalFact - iun={}", notification.getIun())
                .iun(notification.getIun())
                .build();
        logEvent.log();

        try {
            log.debug("Start savePecDeliveryWorkflowLegalFact - iun={}", notification.getIun());

            return this.saveLegalFact(legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                            listFeedbackFromExtChannel, notification, recipient, status, completionWorkflowDate))
                    .map( responseUrl -> {
                        log.debug("End savePecDeliveryWorkflowLegalFact - iun={}", notification.getIun());
                        logEvent.generateSuccess().log();
                        return responseUrl;
                    }).block();
        } catch (Exception exc) {
            logEvent.generateFailure("Error in savePecDeliveryWorkflowLegalFact, exc=", exc).log();

            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "DIGITAL_DELIVERY", notification.getIun(), recipient.getTaxId());
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }
    } 
    

    public Mono<String> saveNotificationViewedLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            Instant timeStamp
    ) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "SaveNotificationViewedLegalFact - iun={}", notification.getIun())
                .iun(notification.getIun())
                .build();
        logEvent.log();
        log.debug("Start saveNotificationViewedLegalFact - iun={}", notification.getIun());

        return Mono.fromCallable(() -> legalFactBuilder.generateNotificationViewedLegalFact(notification.getIun(), recipient, timeStamp))
                .flatMap( res -> {
                        log.info("generateNotificationViewedLegalFact completed - iun={} are not nulls={}", notification.getIun(), res != null);

                        return this.saveLegalFact(res)
                        .map( responseUrl -> {
                            log.debug("End saveNotificationViewedLegalFact - iun={}", notification.getIun());
                            logEvent.generateSuccess().log();
                            return responseUrl;
                        });
                })
                .onErrorResume( err ->
                    generateError(notification, recipient, logEvent, err)
                );
                //.switchIfEmpty( generateError(notification, recipient, logEvent, null));
    }

    @NotNull
    private Mono<String> generateError(NotificationInt notification, NotificationRecipientInt recipient, PnAuditLogEvent logEvent, Throwable err) {
        logEvent.generateFailure("Error in saveNotificationViewedLegalFact,  exc=", err).log();
        String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "NOTIFICATION_VIEWED", notification.getIun(), recipient.getTaxId());
        return Mono.error(new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVENOTIFICATIONFAILED, err));
    }

}
