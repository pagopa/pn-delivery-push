package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.exceptions.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.UpdateFileMetadataRequest;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.unit.DataSize;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ATTACHMENTCHANGESTATUSFAILED;

@Component
@CustomLog
public class AttachmentUtils {
    private static final String VALIDATE_ATTACHMENT_PROCESS = "Validate attachment";
    private static final String F24_URL_PREFIX = "f24set:///";
    private final SafeStorageService safeStorageService;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private final NotificationProcessCostService notificationProcessCostService;

    public AttachmentUtils(SafeStorageService safeStorageService, PnDeliveryPushConfigs pnDeliveryPushConfigs, NotificationProcessCostService notificationProcessCostService) {
        this.safeStorageService = safeStorageService;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.notificationProcessCostService = notificationProcessCostService;
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
            if(recipient.getPayments() != null) {
                recipient.getPayments().forEach(
                        payment -> {
                            if(payment.getPagoPA() != null && payment.getPagoPA().getAttachment() != null) {
                                callback.accept(payment.getPagoPA().getAttachment());
                            }

                            if(payment.getF24() != null && payment.getF24().getMetadataAttachment() != null) {
                                callback.accept(payment.getF24().getMetadataAttachment());
                            }
                        }
                );
            }
        }
    }

    private List<NotificationDocumentInt> getAllAttachment(NotificationInt notification)
    {
        List<NotificationDocumentInt> notificationDocuments = new ArrayList<>(notification.getDocuments());

        notification.getRecipients().forEach( recipient -> {
            addAllRecipientPaymentsToAttachmentList(notificationDocuments, recipient);
        });
        
        return notificationDocuments;
    }

    private void addAllRecipientPaymentsToAttachmentList(List<NotificationDocumentInt> notificationDocuments, NotificationRecipientInt recipient) {
        if(recipient.getPayments() != null) {
            recipient.getPayments().forEach(payment -> {
                if(payment.getPagoPA() != null && payment.getPagoPA().getAttachment() != null) {
                    notificationDocuments.add(payment.getPagoPA().getAttachment());
                }

                if(payment.getF24() != null && payment.getF24().getMetadataAttachment() != null) {
                    notificationDocuments.add(payment.getF24().getMetadataAttachment());
                }
            });
        }
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

    public List<String> getNotificationAttachments(NotificationInt notification) {
        return notification.getDocuments().stream().map(attachment -> FileUtils.getKeyWithStoragePrefix(attachment.getRef().getKey())).toList();
    }

    public List<String> getNotificationAttachmentsAndPayments(NotificationInt notification, NotificationRecipientInt recipient, Integer recIndex, Boolean isPrepareFlow, List<String> replacedF24AttachmentUrls) {
        List<String> attachments = new ArrayList<>(getNotificationAttachments(notification));
        if (CollectionUtils.isEmpty(recipient.getPayments())) {
            return attachments;
        }

        attachments.addAll(getNotificationPagoPaPayments(recipient));
        if (Boolean.TRUE.equals(isPrepareFlow)) {
            addNotificationF24PaymentsUrl(attachments, notification, recipient, recIndex);
        } else {
            // Se non è flusso prepare, è flusso send e non devo includere la URL degli F24 ma provare ad aggiungere l'eventuale lista di pdf prodotti agli attachments
            if(!CollectionUtils.isEmpty(replacedF24AttachmentUrls)){
                attachments.addAll(replacedF24AttachmentUrls);
            }
        }
        return attachments;
    }

    private List<String> getNotificationPagoPaPayments(NotificationRecipientInt recipient) {
        return recipient.getPayments().stream()
                .filter(notificationPaymentInfoIntV2 -> notificationPaymentInfoIntV2.getPagoPA() != null && notificationPaymentInfoIntV2.getPagoPA().getAttachment() != null)
                .map(payment -> payment.getPagoPA().getAttachment())
                .map(attachment -> FileUtils.getKeyWithStoragePrefix(attachment.getRef().getKey()))
                .toList();

    }

    private void addNotificationF24PaymentsUrl(List<String> attachments, NotificationInt notification, NotificationRecipientInt recipient, Integer recIndex) {
        List<F24Int> f24Payments = recipient.getPayments().stream()
                .map(NotificationPaymentInfoIntV2::getF24)
                .filter(Objects::nonNull)
                .toList();

        //Se non ci sono pagamenti F24 non faccio nulla.
        if(f24Payments.isEmpty()) {
            return;
        }

        boolean f24PaymentsRequireCost = f24Payments.stream().anyMatch(F24Int::getApplyCost);
        Integer cost = null;
        // Se almeno uno dei pagamenti F24 ha applyCost = true, devo calcolare il costo della notifica
        if(f24PaymentsRequireCost) {
            cost = retrieveCost(notification, recIndex);
        }

        attachments.add(getF24Url(notification.getIun(), recIndex, cost));
    }

    private Integer retrieveCost(NotificationInt notificationInt, int recipientIdx) {
        return notificationProcessCostService.notificationProcessCost(notificationInt.getIun(), recipientIdx, notificationInt.getNotificationFeePolicy(), true, notificationInt.getPaFee())
                    .map(NotificationProcessCost::getCost)
                    .block();
    }

    private boolean checkIsPDF(byte[] data) {
        if (data == null || data.length < 4)
            return false;

        // check header
        return data[0] == 0x25 && // %
                data[1] == 0x50 && // P
                data[2] == 0x44 && // D
                data[3] == 0x46 && // F
                data[4] == 0x2D;   // -
    }

    public String getF24Url(String iun, Integer recIndex, Integer cost) {
        StringBuilder stringBuilder = new StringBuilder(F24_URL_PREFIX);
        stringBuilder.append(iun);
        stringBuilder.append("/");
        stringBuilder.append(recIndex);

        if (cost != null && cost > 0) {
            stringBuilder.append("?cost=");
            stringBuilder.append(cost);
        }
        return stringBuilder.toString();
    }
}
