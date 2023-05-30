package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.exceptions.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED;

@Component
@CustomLog
public class AttachmentUtils {
    private static final String VALIDATE_ATTACHMENT_PROCESS = "Validate attachment";
    private final SafeStorageService safeStorageService;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public AttachmentUtils(SafeStorageService safeStorageService, PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.safeStorageService = safeStorageService;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }
    
    public void validateAttachment(NotificationInt notification ) throws PnValidationException {
        log.logChecking(VALIDATE_ATTACHMENT_PROCESS);
        forEachAttachment(notification, this::checkAttachment);
        log.logCheckingOutcome(VALIDATE_ATTACHMENT_PROCESS, true);
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
            if(recipient.getPayment() != null && recipient.getPayment().getPagoPaForm() != null){
                    callback.accept(recipient.getPayment().getPagoPaForm());

            }
        }
    }

    private List<NotificationDocumentInt> getAllAttachment(NotificationInt notification)
    {
        List<NotificationDocumentInt> notificationDocuments = new ArrayList<>(notification.getDocuments());

        notification.getRecipients().forEach( recipient -> {
            if(recipient.getPayment() != null && recipient.getPayment().getPagoPaForm() != null){
                    notificationDocuments.add(recipient.getPayment().getPagoPaForm());
            }
        });
        
        return notificationDocuments;
    }

    private void checkAttachment(NotificationDocumentInt attachment) {
        NotificationDocumentInt.Ref ref = attachment.getRef();

        FileDownloadResponseInt fd = MDCUtils.addMDCToContextAndExecute(
                safeStorageService.getFile(ref.getKey(),false)
                        .onErrorResume(PnFileNotFoundException.class, this::handleNotFoundError)
        ).block();

        if(fd != null){
            String attachmentKey = fd.getKey();
            log.debug( "Check preload digest for attachment with key={}", attachmentKey);
            if ( !attachment.getDigests().getSha256().equals( fd.getChecksum() )) {
                final String errorDetail = "Validation failed, different sha256 expected=" + attachment.getDigests().getSha256()
                        + " actual=" + fd.getChecksum();
                log.logCheckingOutcome(VALIDATE_ATTACHMENT_PROCESS, false, errorDetail);
                
                throw new PnValidationNotMatchingShaException(errorDetail);
            }

            // check della size, con -1 si intende disabilitata
            if ( !pnDeliveryPushConfigs.getCheckPdfSize().isNegative() && pnDeliveryPushConfigs.getCheckPdfSize().toBytes() < fd.getContentLength().longValue() ) {
                final String errorDetail = "Validation failed, file too big, max expected=" +  pnDeliveryPushConfigs.getCheckPdfSize()
                        + "  actual=" + DataSize.ofBytes(fd.getContentLength().longValue());
                log.logCheckingOutcome(VALIDATE_ATTACHMENT_PROCESS, false, errorDetail);

                throw new PnValidationPDFTooBigValidException(errorDetail);
            }

            // check del contenuto del documento, che sia un PDF
            if (pnDeliveryPushConfigs.isCheckPdfValidEnabled()) {
                // scarico una porzione di pdf (per fare il check per ora, mi interessa controllare che inizi per %PDF-)
                byte[] pieceOfPdf = this.safeStorageService.downloadPieceOfContent(fd.getKey(), fd.getDownload().getUrl(), 1024).block();

                if ( !checkIsPDF(pieceOfPdf) )
                {
                    final String errorDetail = "Validation failed, file pdf check failed";
                    log.logCheckingOutcome(VALIDATE_ATTACHMENT_PROCESS, false, errorDetail);

                    throw new PnValidationPDFNotValidException(errorDetail);
                }
            }


        } else {
            final String errorDetail = "Validation failed, different sha256 expected=" + attachment.getDigests().getSha256()
                    + " actual=" + null;
            log.logCheckingOutcome(VALIDATE_ATTACHMENT_PROCESS, false, errorDetail);
            
            throw new PnValidationNotMatchingShaException(errorDetail);
        }
    }


    @NotNull
    private Mono<FileDownloadResponseInt> handleNotFoundError(PnFileNotFoundException ex) {
        log.logCheckingOutcome(VALIDATE_ATTACHMENT_PROCESS, false, ex.getMessage());
        return Mono.error(
                new PnValidationFileNotFoundException(
                        ex.getMessage(),
                        ex.getCause()
                )
        );
    }

    private void changeAttachmentStatusToAttached(NotificationDocumentInt attachment) {
        NotificationDocumentInt.Ref ref = attachment.getRef();
        final String ATTACHED_STATUS = "ATTACHED";
        log.debug( "changeAttachmentStatusToAttached begin changing status for attachment with key={}", ref.getKey());

        MDCUtils.addMDCToContextAndExecute(
                updateFileMetadata(ref.getKey(), ATTACHED_STATUS, null)
                        .doOnSuccess( res -> log.info( "changeAttachmentStatusToAttached changed status for attachment with key={}", ref.getKey()))
        ).block();
        
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

    private List<NotificationDocumentInt> getAllAttachmentByRecipient(NotificationInt notification, NotificationRecipientInt recipient)
    {
        List<NotificationDocumentInt> notificationDocuments = new ArrayList<>(notification.getDocuments());

        if(recipient.getPayment() != null && recipient.getPayment().getPagoPaForm() != null){
                notificationDocuments.add(recipient.getPayment().getPagoPaForm());
        }

        return notificationDocuments;
    }

    public List<String> getNotificationAttachments(NotificationInt notification, NotificationRecipientInt recipient) {
        return getAllAttachmentByRecipient(notification, recipient).stream().map(attachment -> FileUtils.getKeyWithStoragePrefix(attachment.getRef().getKey())).toList();
    }

    private boolean checkIsPDF(byte[] data)
    {
        if (data == null || data.length < 4)
            return false;

        // check header
        return data[0] == 0x25 && // %
                data[1] == 0x50 && // P
                data[2] == 0x44 && // D
                data[3] == 0x46 && // F
                data[4] == 0x2D;   // -
    }
}
