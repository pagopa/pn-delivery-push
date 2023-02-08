package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationFileNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotMatchingShaException;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED;

@Component
@Slf4j
public class AttachmentUtils {
    private final SafeStorageService safeStorageService;

    public AttachmentUtils(SafeStorageService safeStorageService) {
        this.safeStorageService = safeStorageService;
    }
    
    public void validateAttachment(NotificationInt notification ) throws PnValidationException {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VALID, "Check attachment for iun={}", notification.getIun() )
                .iun(notification.getIun())
                .build();
        logEvent.log();
        
        try {
            forEachAttachment(notification, this::checkAttachment);

            logEvent.generateSuccess().log();
        } catch (PnValidationException ex) {
            logEvent.generateFailure("check attachment Failed exc={}", ex);
            throw ex;
        }
    }

    public void changeAttachmentsStatusToAttached(NotificationInt notification ) {
        log.info( "changeAttachmentsStatusToAttached iun={}", notification.getIun());

        forEachAttachment(notification, this::changeAttachmentStatusToAttached);
    }

    public Flux<Void> changeAttachmentsRetention(NotificationInt notification, int retentionUntilDays) {
        log.info( "changeAttachmentsRetention iun={}", notification.getIun());
        return Mono.just(getAllAttachment(notification))
                .flatMapIterable( x -> x )
                .flatMap( doc -> this.changeAttachmentRetention(doc, retentionUntilDays));
    }

    private void forEachAttachment(NotificationInt notification, Consumer<NotificationDocumentInt> callback)
    {
        for(NotificationDocumentInt attachment : notification.getDocuments()) {
            callback.accept(attachment);
        }

        for(NotificationRecipientInt recipient : notification.getRecipients()) {
            if(recipient.getPayment() != null ){

                if(recipient.getPayment().getPagoPaForm() != null){
                    callback.accept(recipient.getPayment().getPagoPaForm());
                }
                if(recipient.getPayment().getF24flatRate() != null){
                    callback.accept(recipient.getPayment().getF24flatRate());
                }

            }
        }
    }

    private List<NotificationDocumentInt> getAllAttachment(NotificationInt notification)
    {
        List<NotificationDocumentInt> notificationDocuments = new ArrayList<>(notification.getDocuments());

        notification.getRecipients().forEach( recipient -> {
            if(recipient.getPayment() != null ){

                if(recipient.getPayment().getPagoPaForm() != null){
                    notificationDocuments.add(recipient.getPayment().getPagoPaForm());
                }
                if(recipient.getPayment().getF24flatRate() != null){
                    notificationDocuments.add(recipient.getPayment().getF24flatRate());
                }
            }
        });
        
        return notificationDocuments;
    }

    private void checkAttachment(NotificationDocumentInt attachment) {
        NotificationDocumentInt.Ref ref = attachment.getRef();
        FileDownloadResponseInt fd = null;
        try {
            fd = safeStorageService.getFile(ref.getKey(),true).block();
        } catch ( PnNotFoundException ex ) {
            throw new PnValidationFileNotFoundException(
                    NotificationValidationActionHandler.FILE_NOTFOUND,
                    ex.getProblem().getDetail(),
                    ex 
            );
        }
        
        if(fd != null){
            String attachmentKey = fd.getKey();
            log.debug( "Check preload digest for attachment with key={}", attachmentKey);
            if ( !attachment.getDigests().getSha256().equals( fd.getChecksum() )) {
                throw new PnValidationNotMatchingShaException( NotificationValidationActionHandler.FILE_SHA_ERROR,
                        "Validation failed, different sha256 expected="+ attachment.getDigests().getSha256()
                                + " actual="+ fd.getChecksum() );
            }
        } else{
            throw new PnValidationNotMatchingShaException( NotificationValidationActionHandler.FILE_SHA_ERROR,
                    "Validation failed, different sha256 expected="+ attachment.getDigests().getSha256()
                            + " actual="+ null );
        }
    }

    private void changeAttachmentStatusToAttached(NotificationDocumentInt attachment) {
        NotificationDocumentInt.Ref ref = attachment.getRef();
        final String ATTACHED_STATUS = "ATTACHED";
        log.debug( "changeAttachmentStatusToAttached begin changing status for attachment with key={}", ref.getKey());
        
        updateFileMetadata(ref.getKey(), ATTACHED_STATUS, null)
                .doOnSuccess( res -> log.info( "changeAttachmentStatusToAttached changed status for attachment with key={}", ref.getKey()))
                .block();
    }

    private Mono<Void> changeAttachmentRetention(NotificationDocumentInt attachment, int retentionUntilDays) {
        NotificationDocumentInt.Ref ref = attachment.getRef();
        OffsetDateTime retentionUntil = OffsetDateTime.now().plus(retentionUntilDays, ChronoUnit.DAYS);
        log.info( "changeAttachmentRetention begin changing retentionUntil for attachment with key={}", ref.getKey());

        return updateFileMetadata(ref.getKey(), null, retentionUntil);
    }

    private Mono<Void> updateFileMetadata(String fileKey, String statusRequest, OffsetDateTime retentionUntilRequest) {
        UpdateFileMetadataRequest request = new UpdateFileMetadataRequest();
        request.setStatus(statusRequest);
        request.setRetentionUntil(retentionUntilRequest);

        return safeStorageService.updateFileMetadata(fileKey, request)
                .flatMap( fd -> {
                    log.info( "Response updateFileMetadata returned={}",fd);

                    if (fd != null && !fd.getResultCode().startsWith("2"))
                    {
                        // è un FAIL
                        log.error("Cannot change metadata for attachment key={} result={}", fileKey, fd);
                        return Mono.error(new PnInternalException("Failed update metadata attachment", ERROR_CODE_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED));
                    }

                    return Mono.empty();

                });
    }
}
