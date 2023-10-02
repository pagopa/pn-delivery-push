package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.F24Int;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoIntV2;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.PagoPaInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class PaymentMapper {
    private PaymentMapper() {
    }

    @NotNull
    public static List<NotificationPaymentItem> getNotificationPaymentItem(List<NotificationPaymentInfoIntV2> paymentInternalList) {

        if (!CollectionUtils.isEmpty(paymentInternalList)) {
            return paymentInternalList.stream()
                    .map(paymentInfoIntV2 -> {
                        NotificationPaymentItem item = new NotificationPaymentItem();
                        item.setF24(getF24(paymentInfoIntV2.getF24()));
                        item.setPagoPa(getPagoPa(paymentInfoIntV2.getPagoPA()));
                        return item;
                    })
                    .toList();

        }
        return Collections.emptyList();
    }

    @NotNull
    public static List<NotificationPaymentInfoIntV2> getNotificationPaymentInfo(List<NotificationPaymentItem> paymentItemList) {

        if (!CollectionUtils.isEmpty(paymentItemList)) {
            return paymentItemList.stream()
                    .map(item ->
                            NotificationPaymentInfoIntV2.builder()
                                    .f24(getF24Int(item.getF24()))
                                    .pagoPA(getPagoPaInt(item.getPagoPa()))
                                    .build())
                    .toList();

        }
        return Collections.emptyList();
    }

    private static F24Payment getF24(F24Int f24) {
        F24Payment f24Payment = null;
        if (f24 != null) {
            f24Payment = new F24Payment();
            f24Payment.setApplyCost(f24.getApplyCost());
            f24Payment.setTitle(f24.getTitle());
            f24Payment.setMetadataAttachment(getNotificationMetadataAttachment(f24.getMetadataAttachment()));
        }
        return f24Payment;
    }

    private static F24Int getF24Int(F24Payment f24) {
        if(f24 != null) {
            return F24Int.builder()
                    .metadataAttachment(getNotificationDocumentInt(f24.getMetadataAttachment()))
                    .title(f24.getTitle())
                    .applyCost(f24.getApplyCost())
                    .build();
        }
        return null;
    }

    private static PagoPaPayment getPagoPa(PagoPaInt pagoPA) {
        PagoPaPayment pagoPaPayment = null;
        if (pagoPA != null) {
            pagoPaPayment = new PagoPaPayment();
            pagoPaPayment.setApplyCost(pagoPA.getApplyCost());
            pagoPaPayment.setAttachment(getNotificationPaymentAttachment(pagoPA.getAttachment()));
            pagoPaPayment.setCreditorTaxId(pagoPA.getCreditorTaxId());
            pagoPaPayment.setNoticeCode(pagoPA.getNoticeCode());
        }
        return pagoPaPayment;
    }


    private static PagoPaInt getPagoPaInt(PagoPaPayment pagoPa) {
        if(pagoPa != null) {
            return PagoPaInt.builder()
                    .applyCost(pagoPa.getApplyCost())
                    .attachment(getNotificationDocumentInt(pagoPa.getAttachment()))
                    .creditorTaxId(pagoPa.getCreditorTaxId())
                    .noticeCode(pagoPa.getNoticeCode())
                    .build();
        }
        return null;
    }

    private static NotificationMetadataAttachment getNotificationMetadataAttachment(NotificationDocumentInt metadataAttachment) {
        NotificationMetadataAttachment notificationMetadataAttachment = new NotificationMetadataAttachment();
        notificationMetadataAttachment.setDigests(getAttachmentDigest(metadataAttachment.getDigests()));
        notificationMetadataAttachment.setRef(getRef(metadataAttachment.getRef()));
        return notificationMetadataAttachment;
    }

    private static NotificationPaymentAttachment getNotificationPaymentAttachment(NotificationDocumentInt attachment) {
        NotificationPaymentAttachment notificationPaymentAttachment = new NotificationPaymentAttachment();
        notificationPaymentAttachment.setDigests(getAttachmentDigest(attachment.getDigests()));
        notificationPaymentAttachment.setRef(getRef(attachment.getRef()));
        return notificationPaymentAttachment;
    }

    private static NotificationDocumentInt getNotificationDocumentInt(NotificationMetadataAttachment metadataAttachment) {
        return NotificationDocumentInt.builder()
                .digests(getDigestInt(metadataAttachment.getDigests()))
                .ref(getRefInt(metadataAttachment.getRef()))
                .build();
    }

    private static NotificationDocumentInt getNotificationDocumentInt(NotificationPaymentAttachment attachment) {
        if(attachment != null) {
            return NotificationDocumentInt.builder()
                    .ref(getRefInt(attachment.getRef()))
                    .digests(getDigestInt(attachment.getDigests()))
                    .build();
        }
        return null;
    }

    private static NotificationAttachmentDigests getAttachmentDigest(NotificationDocumentInt.Digests digests) {
        NotificationAttachmentDigests notificationAttachmentDigests = new NotificationAttachmentDigests();
        notificationAttachmentDigests.setSha256(digests.getSha256());
        return notificationAttachmentDigests;
    }

    private static NotificationDocumentInt.Digests getDigestInt(NotificationAttachmentDigests digests) {
        return NotificationDocumentInt.Digests.builder()
                .sha256(digests.getSha256())
                .build();
    }

    private static NotificationAttachmentBodyRef getRef(NotificationDocumentInt.Ref ref) {
        NotificationAttachmentBodyRef notificationAttachmentBodyRef = new NotificationAttachmentBodyRef();
        notificationAttachmentBodyRef.setKey(ref.getKey());
        notificationAttachmentBodyRef.setVersionToken(ref.getVersionToken());
        return notificationAttachmentBodyRef;
    }

    private static NotificationDocumentInt.Ref getRefInt(NotificationAttachmentBodyRef ref) {
        return NotificationDocumentInt.Ref.builder()
                .key(ref.getKey())
                .versionToken(ref.getVersionToken())
                .build();
    }
}
