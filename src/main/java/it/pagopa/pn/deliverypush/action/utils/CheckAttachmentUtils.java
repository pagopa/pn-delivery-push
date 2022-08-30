package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.validator.NotificationReceiverValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CheckAttachmentUtils {
    private final NotificationReceiverValidator validator;
    private final SafeStorageService safeStorageService;

    public CheckAttachmentUtils(NotificationReceiverValidator validator, SafeStorageService safeStorageService) {
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
}