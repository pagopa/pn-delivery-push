package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class NotificationMapper {
    private NotificationMapper(){}

    public static NotificationInt externalToInternal(SentNotification sentNotification) {

        List<NotificationRecipientInt> listNotificationRecipientInt = mapNotificationRecipient(sentNotification.getRecipients());
        List<NotificationDocumentInt> listNotificationDocumentIntInt = mapNotificationDocument(sentNotification.getDocuments());

        ServiceLevelTypeInt lvl = null;
        if( sentNotification.getPhysicalCommunicationType() != null ) {
            lvl = ServiceLevelTypeInt.valueOf( sentNotification.getPhysicalCommunicationType().name() );
        }

        return NotificationInt.builder()
                .iun(sentNotification.getIun())
                .paNotificationId(sentNotification.getPaProtocolNumber())
                .physicalCommunicationType( lvl )
                .sentAt(sentNotification.getSentAt())
                .sender(
                        NotificationSenderInt.builder()
                                .paTaxId( sentNotification.getSenderTaxId() )
                                .paId(sentNotification.getSenderPaId())
                                .paDenomination(sentNotification.getSenderDenomination())
                                .paTaxId(sentNotification.getSenderTaxId())
                                .build()
                )
                .documents(listNotificationDocumentIntInt)
                .recipients(listNotificationRecipientInt)
                .build();
    }

    private static List<NotificationDocumentInt> mapNotificationDocument(List<NotificationDocument> documents) {
        List<NotificationDocumentInt> list = new ArrayList<>();

        for (NotificationDocument document : documents){
            NotificationDocumentInt notificationDocumentInt = NotificationDocumentInt.builder()
                    .digests(
                            NotificationDocumentInt.Digests.builder()
                                    .sha256(document.getDigests().getSha256())
                                    .build()
                    )
                    .ref(
                            NotificationDocumentInt.Ref.builder()
                                    .key(document.getRef().getKey())
                                    .versionToken(document.getRef().getVersionToken())
                                    .build()
                    )
                    .build();

            list.add(notificationDocumentInt);
        }

        return list;
    }

    private static List<NotificationRecipientInt> mapNotificationRecipient(List<NotificationRecipient> recipients) {
        List<NotificationRecipientInt> list = new ArrayList<>();

        for (NotificationRecipient recipient : recipients){
            NotificationRecipientInt.NotificationRecipientIntBuilder notificationRecIntBuilder = NotificationRecipientInt
                    .builder()
                    .taxId(recipient.getTaxId())
                    .denomination(recipient.getDenomination());

            it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationDigitalAddress digitalDomicile = recipient.getDigitalDomicile();
            if(digitalDomicile != null){
                DigitalAddress.TypeEnum typeEnum = DigitalAddress.TypeEnum.valueOf(digitalDomicile.getType().name());

                notificationRecIntBuilder
                        .digitalDomicile(
                                DigitalAddress.builder()
                                        .address(digitalDomicile.getAddress())
                                        .type(typeEnum)
                                        .build()
                        );
            }

            NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();
            if(physicalAddress != null){
                notificationRecIntBuilder
                        .physicalAddress(
                                PhysicalAddress.builder()
                                        .at(physicalAddress.getAt())
                                        .address(physicalAddress.getAddress())
                                        .addressDetails(physicalAddress.getAddressDetails())
                                        .foreignState(physicalAddress.getForeignState())
                                        .municipality(physicalAddress.getMunicipality())
                                        .municipalityDetails(physicalAddress.getMunicipalityDetails())
                                        .province(physicalAddress.getProvince())
                                        .zip(physicalAddress.getZip())
                                        .build()
                        );
            }

            it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationPaymentInfo payment = recipient.getPayment();

            if(payment != null){
                NotificationPaymentInfoInt.NotificationPaymentInfoIntBuilder paymentInfoBuilder = NotificationPaymentInfoInt.builder()
                        .pagoPaForm(
                                NotificationDocumentInt.builder()
                                        .digests(
                                                NotificationDocumentInt.Digests.builder()
                                                        .sha256(payment.getPagoPaForm().getDigests().getSha256())
                                                        .build()
                                        )
                                        .ref(
                                                NotificationDocumentInt.Ref.builder()
                                                        .key(payment.getPagoPaForm().getRef().getKey())
                                                        .versionToken(payment.getPagoPaForm().getRef().getVersionToken())
                                                        .build()
                                        )
                                        .build()
                        );

                if(payment.getF24flatRate() != null){
                    paymentInfoBuilder
                            .f24flatRate(
                                    NotificationDocumentInt.builder()
                                            .digests(
                                                    NotificationDocumentInt.Digests.builder()
                                                            .sha256(payment.getF24flatRate().getDigests().getSha256())
                                                            .build()
                                            )
                                            .ref(
                                                    NotificationDocumentInt.Ref.builder()
                                                            .key(payment.getF24flatRate().getRef().getKey())
                                                            .versionToken(payment.getF24flatRate().getRef().getVersionToken())
                                                            .build()
                                            )
                                            .build()
                            );
                }
                notificationRecIntBuilder.payment(paymentInfoBuilder.build());
            }
            list.add(notificationRecIntBuilder.build());
        }
        return list;
    }



    public static SentNotification internalToExternal(NotificationInt notification) {
        SentNotification sentNotification = new SentNotification();

        sentNotification.setIun(notification.getIun());
        sentNotification.setPaProtocolNumber(notification.getPaNotificationId());
        sentNotification.setSentAt(notification.getSentAt());

        if( notification.getPhysicalCommunicationType() != null ) {
            sentNotification.setPhysicalCommunicationType(
                    SentNotification.PhysicalCommunicationTypeEnum.valueOf( notification.getPhysicalCommunicationType().name() )
            );
        }

        NotificationSenderInt sender = notification.getSender();
        if( sender != null ) {
            sentNotification.setSenderDenomination( sender.getPaDenomination() );
            sentNotification.setSenderPaId( sender.getPaId() );
            sentNotification.setSenderTaxId( sender.getPaTaxId() );
        }

        List<NotificationRecipient> recipients = notification.getRecipients().stream()
                .map(NotificationMapper::getNotificationRecipient).collect(Collectors.toList());

        sentNotification.setRecipients(recipients);

        List<NotificationDocument> documents = notification.getDocuments().stream().map(
                NotificationMapper::getNotificationDocument).collect(Collectors.toList());

        sentNotification.setDocuments(documents);

        if(notification.getPhysicalCommunicationType() != null){
            sentNotification.setPhysicalCommunicationType(SentNotification.PhysicalCommunicationTypeEnum.valueOf(notification.getPhysicalCommunicationType().name()));
        }
        
        if(notification.getSender() != null){
            sentNotification.setSenderPaId(notification.getSender().getPaId());
            sentNotification.setSenderDenomination(notification.getSender().getPaDenomination());
            sentNotification.setSenderTaxId(notification.getSender().getPaTaxId());
        }
        
        return sentNotification;
    }

    @NotNull
    private static NotificationDocument getNotificationDocument(NotificationDocumentInt documentInt) {
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
    private static NotificationRecipient getNotificationRecipient(NotificationRecipientInt recipient) {
        NotificationRecipient notificationRecipient = new NotificationRecipient();
        NotificationDigitalAddress notificationDigitalAddress = null;

        DigitalAddress internalDigitalDomicile = recipient.getDigitalDomicile();
        if(internalDigitalDomicile != null){
            notificationDigitalAddress = new NotificationDigitalAddress();
            notificationDigitalAddress.setAddress(internalDigitalDomicile.getAddress());
            notificationDigitalAddress.setType(NotificationDigitalAddress.TypeEnum.valueOf(internalDigitalDomicile.getType().getValue()));
        }

        NotificationPhysicalAddress physicalAddress = null;
        PhysicalAddress internalPhysicalAddress = recipient.getPhysicalAddress();
        if(internalPhysicalAddress != null){
            physicalAddress = new NotificationPhysicalAddress();
            physicalAddress.setAddress(internalPhysicalAddress.getAddress());
            physicalAddress.setAddressDetails(internalPhysicalAddress.getAddressDetails());
            physicalAddress.setAt(internalPhysicalAddress.getAt());
            physicalAddress.setMunicipality(internalPhysicalAddress.getMunicipality());
            physicalAddress.setForeignState(internalPhysicalAddress.getForeignState());
            physicalAddress.setProvince(internalPhysicalAddress.getProvince());
            physicalAddress.setZip(internalPhysicalAddress.getZip());
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
    
}
