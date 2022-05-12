package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.externalclient.pnclient.delivery.PnDeliveryClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PnDeliveryClientMock implements PnDeliveryClient {
    private Collection<SentNotification> notifications;

    public void clear() {
        this.notifications = new ArrayList<>();
    }

    public void addNotification(NotificationInt notification) {
        SentNotification sentNotification = new SentNotification();
        
        sentNotification.setIun(notification.getIun());
        sentNotification.setPaProtocolNumber(notification.getPaNotificationId());
        sentNotification.setSentAt(notification.getSentAt());

        List<NotificationRecipient> recipients = notification.getRecipients().stream()
                .map(this::getNotificationRecipient).collect(Collectors.toList());

        sentNotification.setRecipients(recipients);

        List<NotificationDocument> documents = notification.getDocuments().stream().map(
                this::getNotificationDocument).collect(Collectors.toList());
        
        sentNotification.setDocuments(documents);
        
        if(notification.getPhysicalCommunicationType() != null){
            sentNotification.setPhysicalCommunicationType(SentNotification.PhysicalCommunicationTypeEnum.valueOf(notification.getPhysicalCommunicationType().name()));
        }

        notifications.add(sentNotification);
    }

    @NotNull
    private NotificationDocument getNotificationDocument(NotificationDocumentInt documentInt) {
        NotificationAttachmentDigests digests = new NotificationAttachmentDigests();
        digests.setSha256(documentInt.getDigests().getSha256());

        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey(documentInt.getRef().getKey());
        ref.setVersionToken(documentInt.getRef().getVersionToken());

        NotificationDocument document = new NotificationDocument();
        document.setDigests(digests);
        document.setRef(ref);
        return document;
    }

    @NotNull
    private NotificationRecipient getNotificationRecipient(NotificationRecipientInt recipient) {
        NotificationRecipient notificationRecipient = new NotificationRecipient();
        NotificationDigitalAddress notificationDigitalAddress = null;

        DigitalAddress internalDigitalDomicile = recipient.getDigitalDomicile();
        if(internalDigitalDomicile != null){
            notificationDigitalAddress = new NotificationDigitalAddress();
            notificationDigitalAddress.setAddress(internalDigitalDomicile.getAddress());
            if(internalDigitalDomicile.getType() != null){
                notificationDigitalAddress.setType(NotificationDigitalAddress.TypeEnum.valueOf(internalDigitalDomicile.getType().getValue()));
            }
        }

        NotificationPhysicalAddress physicalAddress = null;
        PhysicalAddress internalPhysicalAddress = recipient.getPhysicalAddress();
        if(internalPhysicalAddress != null){
            physicalAddress = new NotificationPhysicalAddress();
            physicalAddress.setAddress(internalPhysicalAddress.getAddress());
            physicalAddress.setAddressDetails(internalPhysicalAddress.getAddressDetails());
            physicalAddress.setAt(physicalAddress.getAt());
            physicalAddress.setMunicipality(physicalAddress.getMunicipality());
            physicalAddress.setForeignState(physicalAddress.getForeignState());
            physicalAddress.setProvince(physicalAddress.getProvince());
        }

        NotificationPaymentInfo payment = null;
        NotificationPaymentInfoInt paymentInternal = recipient.getPayment();
        if(paymentInternal != null){
            payment = new NotificationPaymentInfo();

            NotificationPaymentAttachment pagoPaForm = null;
            if(paymentInternal.getPagoPaForm() != null){
                NotificationDocumentInt pagoPaFormInternal = paymentInternal.getPagoPaForm();
                pagoPaForm = new NotificationPaymentAttachment();

                NotificationAttachmentDigests digests = new NotificationAttachmentDigests();
                digests.setSha256(pagoPaFormInternal.getDigests().getSha256());
                pagoPaForm.setDigests(digests);

                NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
                ref.setKey(pagoPaFormInternal.getRef().getKey());
                ref.setVersionToken(pagoPaFormInternal.getRef().getVersionToken());
                pagoPaForm.setRef(ref);
            }
            payment.setPagoPaForm(pagoPaForm);

            NotificationPaymentAttachment f24White = null;
            if (paymentInternal.getF24white() != null) {
                NotificationDocumentInt f24flatRateInternal = paymentInternal.getF24white();

                f24White = new NotificationPaymentAttachment();

                NotificationAttachmentDigests digests = new NotificationAttachmentDigests();
                digests.setSha256(f24flatRateInternal.getDigests().getSha256());
                f24White.setDigests(digests);

                NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
                ref.setKey(f24flatRateInternal.getRef().getKey());
                ref.setVersionToken(f24flatRateInternal.getRef().getVersionToken());
                f24White.setRef(ref);
            }
            payment.setF24white(f24White);

            NotificationPaymentAttachment f24flatRate = null;
            if (paymentInternal.getF24flatRate() != null){
                NotificationDocumentInt f24FlatRateInternal = paymentInternal.getF24flatRate();

                f24flatRate = new NotificationPaymentAttachment();

                NotificationAttachmentDigests digests = new NotificationAttachmentDigests();
                digests.setSha256(f24FlatRateInternal.getDigests().getSha256());
                f24flatRate.setDigests(digests);

                NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
                ref.setKey(f24FlatRateInternal.getRef().getKey());
                ref.setVersionToken(f24FlatRateInternal.getRef().getVersionToken());
                f24flatRate.setRef(ref);
            }
            payment.setF24flatRate(f24flatRate);
        }

        notificationRecipient.setTaxId(recipient.getTaxId());
        notificationRecipient.setDenomination(recipient.getDenomination());
        notificationRecipient.setDigitalDomicile(notificationDigitalAddress);
        notificationRecipient.setPhysicalAddress(physicalAddress);
        notificationRecipient.setPayment(payment);

        return notificationRecipient;
    }

    @Override
    public ResponseEntity<Void> updateStatus(it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.RequestUpdateStatusDto dto) {
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<SentNotification> getSentNotification(String iun) {
        Optional<SentNotification> sentNotificationOpt = notifications.stream().filter(notification -> iun.equals(notification.getIun())).findFirst();
        if(sentNotificationOpt.isPresent()){
            return ResponseEntity.ok(sentNotificationOpt.get());
        }
        throw new RuntimeException("Test error, iun is not presente in getSentNotification");
    }
}
