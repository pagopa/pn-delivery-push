package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.externalclient.pnclient.safestorage.datavault.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.validator.NotificationReceiverValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CheckAttachmentUtils {
    private final NotificationReceiverValidator validator;
    private final PnSafeStorageClient safeStorageClient;

    public CheckAttachmentUtils(NotificationReceiverValidator validator, PnSafeStorageClient safeStorageClient) {
        this.validator = validator;
        this.safeStorageClient = safeStorageClient;
    }
    
    public void validateAttachment(NotificationInt notification ) throws PnValidationException {
        log.debug( "Start check attachment for document" );
        for(NotificationDocumentInt attachment : notification.getDocuments()) {
            checkAttachment(attachment);
        }
        log.debug( "End check attachment for document" );

        notification.getRecipients().forEach(
                recipient -> checkPayment(recipient.getPayment())
        );
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

        FileDownloadResponse fd = safeStorageClient.getFile(ref.getKey(),true);

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