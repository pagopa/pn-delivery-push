package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationPaymentInfo;
import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.validator.NotificationReceiverValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class CheckAttachmentUtils {
    private final NotificationReceiverValidator validator;
    private final FileStorage fileStorage;

    public CheckAttachmentUtils(NotificationReceiverValidator validator, FileStorage fileStorage) {
        this.validator = validator;
        this.fileStorage = fileStorage;
    }
    
    public void validateAttachment(Notification notification ) throws PnValidationException {
        log.debug( "Start check attachment for document" );
        for(NotificationAttachment attachment : notification.getDocuments()) {
            checkAttachment(attachment);
        }
        log.debug( "End check attachment for document" );
       
       if(notification.getPayment() != null && notification.getPayment().getF24() != null){
           log.debug( "Start check attachment for payment" );

           NotificationPaymentInfo.F24 f24 = notification.getPayment().getF24();
           if(f24.getAnalog() != null){
               checkAttachment(f24.getAnalog());
           }
           if(f24.getDigital() != null){
               checkAttachment(f24.getDigital());
           }
           if(f24.getFlatRate() != null){
               checkAttachment(f24.getFlatRate());
           }
           
           log.debug( "End check attachment for payment" );
       }
    }

    private void checkAttachment(NotificationAttachment attachment) {
        NotificationAttachment.Ref ref = attachment.getRef();

        FileData fd = fileStorage.getFileVersion( ref.getKey(),ref.getVersionToken() );

        String attachmentKey = fd.getKey();

        try(InputStream contentStream = fd.getContent() ) {

            long startTime = System.currentTimeMillis();
            log.debug( "Compute sha256 for attachment with key={} START", attachmentKey);
            String actualSha256 = DigestUtils.sha256Hex( contentStream );
            long deltaTime = System.currentTimeMillis() - startTime;
            log.debug( "Compute sha256 for attachment with key={} END in={}ms", attachmentKey, deltaTime );

            startTime = System.currentTimeMillis();
            log.debug( "Check preload digest for attachment with key={} START", attachmentKey);
            validator.checkPreloadedDigests(
                    attachmentKey,
                    attachment.getDigests(),
                    NotificationAttachment.Digests.builder()
                            .sha256( actualSha256 )
                            .build()
            );
            log.debug( "Check preload digest for attachment with key={} END in={}ms", attachmentKey,
                    System.currentTimeMillis() - startTime );

            log.debug( "Check attachment digest END" );

        } catch (IOException exc) {
            String msg = "Error validating sha256 for attachment=" + attachmentKey + " version=" + fd.getVersionId();
            log.error( msg );
            throw new PnInternalException( msg, exc );
        }
    }
}