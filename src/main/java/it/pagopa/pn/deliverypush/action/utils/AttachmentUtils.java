package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.validator.NotificationReceiverValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED;

@Component
@Slf4j
public class AttachmentUtils {
    private final NotificationReceiverValidator validator;
    private final SafeStorageService safeStorageService;

    public AttachmentUtils(NotificationReceiverValidator validator, SafeStorageService safeStorageService) {
        this.validator = validator;
        this.safeStorageService = safeStorageService;
    }
    
    public void validateAttachment(NotificationInt notification ) throws PnValidationException {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VALID, "Start check attachment for iun={}", notification.getIun() )
                .iun(notification.getIun())
                .build();
        logEvent.log();
        
        try {
            for(NotificationDocumentInt attachment : notification.getDocuments()) {
                checkAttachment(attachment);
            }

            notification.getRecipients().forEach(
                    recipient -> checkPayment(recipient.getPayment())
            );

            logEvent.generateSuccess().log();
        } catch (PnValidationException ex) {
            logEvent.generateFailure("check attachment Failed exc={}", ex);
            throw ex;
        }
    }


    public void changeAttachmentsStatusToAttached(NotificationInt notification ) {
        log.info( "changeAttachmentsStatusToAttached iun={}", notification.getIun());

        for(NotificationDocumentInt attachment : notification.getDocuments()) {
            changeAttachmentStatusToAttached(attachment);
        }

        for(NotificationRecipientInt recipient : notification.getRecipients()) {
            changePaymentStatusToAttached(recipient.getPayment());
        }
    }

    private void checkPayment(NotificationPaymentInfoInt payment) {
        if(payment != null ){
           log.debug( "Start check attachment for payment" );
           
           if(payment.getPagoPaForm() != null){
               checkAttachment(payment.getPagoPaForm());
           }
           if(payment.getF24flatRate() != null){
               checkAttachment(payment.getF24flatRate());
           }
           
           log.debug( "End check attachment for payment" );
       }
    }


    private void checkAttachment(NotificationDocumentInt attachment) {
        NotificationDocumentInt.Ref ref = attachment.getRef();

        FileDownloadResponseInt fd = safeStorageService.getFile(ref.getKey(),true);

        String attachmentKey = fd.getKey();

        log.debug( "Check preload digest for attachment with key={}", attachmentKey);
        validator.checkPreloadedDigests(
                attachmentKey,
                attachment.getDigests(),
                NotificationDocumentInt.Digests.builder()
                        .sha256( fd.getChecksum() )
                        .build()
        );
    }

    private void changePaymentStatusToAttached(NotificationPaymentInfoInt payment) {
        if(payment != null ){
            log.debug( "Start change attachment for payment" );

            if(payment.getPagoPaForm() != null){
                changeAttachmentStatusToAttached(payment.getPagoPaForm());
            }
            if(payment.getF24flatRate() != null){
                changeAttachmentStatusToAttached(payment.getF24flatRate());
            }

            log.debug( "End change attachment for payment" );
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

        log.debug( "changeAttachmentStatusToAttached changed status for attachment with key={}", ref.getKey());

    }
}