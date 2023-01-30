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
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SAVENOTIFICATIONFAILED;

@Slf4j
@Service
public class SaveLegalFactsServiceImpl implements SaveLegalFactsService {

    public static final String SAVE_LEGAL_FACT_EXCEPTION_MESSAGE = "Generating %s legal fact for IUN=%s and recipientId=%s";
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
                .map( fileCreationResponse -> FileUtils.getKeyWithStoragePrefix(fileCreationResponse.getKey()));
    }

    public PdfInfo sendCreationRequestForAAR(NotificationInt notification,
                                             NotificationRecipientInt recipient,
                                             String quickAccessToken) {
        try {
            log.debug("Start sendCreationRequestForAAR - iun={}", notification.getIun());

            byte[] pdfByte = legalFactBuilder.generateNotificationAAR(notification, recipient, quickAccessToken);
            int numberOfPages = legalFactBuilder.getNumberOfPages(pdfByte);

            FileCreationWithContentRequest fileCreationRequest = new FileCreationWithContentRequest();
            fileCreationRequest.setContentType(LEGALFACTS_MEDIATYPE_STRING);
            fileCreationRequest.setDocumentType(PN_AAR);
            fileCreationRequest.setStatus(SAVED);
            fileCreationRequest.setContent(pdfByte);
            
            return safeStorageService.createAndUploadContent(fileCreationRequest).map( fileCreationResponse ->{
                        PdfInfo pdfInfo = PdfInfo.builder()
                                .key(FileUtils.getKeyWithStoragePrefix(fileCreationResponse.getKey()))
                                .numberOfPages(numberOfPages)
                                .build();

                        log.debug("End sendCreationRequestForAAR - iun={}", notification.getIun());
                        
                        return pdfInfo;
                    }
            ).block();

        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "AAR", notification.getIun(), "N/A");
            log.error("Exception in sendCreationRequestForAAR ex=", exc);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }
    }

    public String sendCreationRequestForNotificationReceivedLegalFact(NotificationInt notification) {
        try {
            log.info("Start sendCreationRequestForNotificationReceivedLegalFact - iun={}", notification.getIun());
            
            return this.saveLegalFact(legalFactBuilder.generateNotificationReceivedLegalFact(notification))
                    .map( responseUrl -> {
                        log.info("sendCreationRequestForNotificationReceivedLegalFact completed with fileKey={} - iun={}", responseUrl, notification.getIun());
                        return responseUrl;
                    }).block();
            
        } catch (Exception exc) {
            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "REQUEST_ACCEPTED", notification.getIun(), "N/A");
            log.error("Exception in saveNotificationReceivedLegalFact ex=", exc);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }

    }

    public String sendCreationRequestForPecDeliveryWorkflowLegalFact(
            List<SendDigitalFeedbackDetailsInt> listFeedbackFromExtChannel,
            NotificationInt notification,
            NotificationRecipientInt recipient,
            EndWorkflowStatus status,
            Instant completionWorkflowDate
    ) {
        //TODO AUDITLOG DA ELIMINARE
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "SavePecDeliveryWorkflowLegalFact - iun={}", notification.getIun())
                .iun(notification.getIun())
                .build();
        logEvent.log();

        try {
            log.debug("Start sendCreationRequestForPecDeliveryWorkflowLegalFact - iun={}", notification.getIun());

            return this.saveLegalFact(legalFactBuilder.generatePecDeliveryWorkflowLegalFact(
                            listFeedbackFromExtChannel, notification, recipient, status, completionWorkflowDate))
                    .map( responseUrl -> {
                        log.debug("End sendCreationRequestForPecDeliveryWorkflowLegalFact - iun={}", notification.getIun());
                        logEvent.generateSuccess().log();
                        return responseUrl;
                    }).block();
        } catch (Exception exc) {
            logEvent.generateFailure("Error in sendCreationRequestForPecDeliveryWorkflowLegalFact, exc=", exc).log();

            String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "DIGITAL_DELIVERY", notification.getIun(), recipient.getTaxId());
            throw new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVELEGALFACTSFAILED, exc);
        }
    } 
    

    public Mono<String> sendCreationRequestForNotificationViewedLegalFact(
            NotificationInt notification,
            NotificationRecipientInt recipient,
            Instant timeStamp
    ) {
        //TODO AUDITLOG DA ELIMINARE
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_NEWLEGAL, "SaveNotificationViewedLegalFact - iun={}", notification.getIun())
                .iun(notification.getIun())
                .build();
        logEvent.log();
        log.debug("Start sendCreationRequestForNotificationViewedLegalFact - iun={}", notification.getIun());

        return Mono.fromCallable(() -> legalFactBuilder.generateNotificationViewedLegalFact(notification.getIun(), recipient, timeStamp))
                .flatMap( res -> {
                        log.info("sendCreationRequestForNotificationViewedLegalFact completed - iun={} are not nulls={}", notification.getIun(), res != null);

                        return this.saveLegalFact(res)
                        .map( responseUrl -> {
                            log.debug("End sendCreationRequestForNotificationViewedLegalFact - iun={}", notification.getIun());
                            logEvent.generateSuccess().log();
                            return responseUrl;
                        });
                })
                .onErrorResume( err ->
                    generateError(notification, recipient, logEvent, err)
                );
    }

    @NotNull
    private Mono<String> generateError(NotificationInt notification, NotificationRecipientInt recipient, PnAuditLogEvent logEvent, Throwable err) {
        logEvent.generateFailure("Error in saveNotificationViewedLegalFact,  exc=", err).log();
        String msg = String.format(SAVE_LEGAL_FACT_EXCEPTION_MESSAGE, "NOTIFICATION_VIEWED", notification.getIun(), recipient.getTaxId());
        return Mono.error(new PnInternalException(msg, ERROR_CODE_DELIVERYPUSH_SAVENOTIFICATIONFAILED, err));
    }

}
