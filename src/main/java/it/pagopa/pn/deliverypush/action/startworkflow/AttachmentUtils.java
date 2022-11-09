package it.pagopa.pn.deliverypush.action.startworkflow;

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
import it.pagopa.pn.deliverypush.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationFileNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotMatchingShaException;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.*;

@Component
@Slf4j
public class AttachmentUtils {
    private final SafeStorageService safeStorageService;
    private final PnAuditLogBuilder auditLogBuilder;

    public AttachmentUtils(SafeStorageService safeStorageService,
                           PnAuditLogBuilder auditLogBuilder) {
        this.safeStorageService = safeStorageService;
        this.auditLogBuilder = auditLogBuilder;
    }
    
    public void validateAttachment(NotificationInt notification ) throws PnValidationException {
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VALID, "Start check attachment for iun={}", notification.getIun() )
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


    private void checkAttachment(NotificationDocumentInt attachment) {
        NotificationDocumentInt.Ref ref = attachment.getRef();
        FileDownloadResponseInt fd = null;
        try {
            fd = safeStorageService.getFile(ref.getKey(),true);
        } catch ( PnNotFoundException ex ) {
            throw new PnValidationFileNotFoundException(
                    ERROR_CODE_DELIVERYPUSH_NOTFOUND,
                    ex.getProblem().getDetail(),
                    ex 
            );
        }

        String attachmentKey = fd.getKey();
        log.debug( "Check preload digest for attachment with key={}", attachmentKey);
        if ( !attachment.getDigests().getSha256().equals( fd.getChecksum() )) {
            throw new PnValidationNotMatchingShaException( ERROR_CODE_DELIVERYPUSH_SHAFILEERROR,
                    "Validation failed, different sha256 expected="+ attachment.getDigests().getSha256()
                            + " actual="+ fd.getChecksum() );
        }
    }

    private void changeAttachmentStatusToAttached(NotificationDocumentInt attachment) {
        NotificationDocumentInt.Ref ref = attachment.getRef();
        log.debug( "changeAttachmentStatusToAttached begin changing status for attachment with key={}", ref.getKey());

        UpdateFileMetadataRequest request = new UpdateFileMetadataRequest();
        request.setStatus("ATTACHED");

        UpdateFileMetadataResponseInt fd = safeStorageService.updateFileMetadata(ref.getKey(), request);

        if (!fd.getResultCode().startsWith("2"))
        {
            // è un FAIL
            log.error("Cannot change attachment status attachment key={} result={}", ref.getKey(), fd);
            throw new PnInternalException("FAiled to mark an attachment as ATTACHED", ERROR_CODE_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED);
        }

        log.info( "changeAttachmentStatusToAttached changed status for attachment with key={}", ref.getKey());

    }
}
