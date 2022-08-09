package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.*;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import org.jetbrains.annotations.NotNull;

public class RecipientMapper {
    private RecipientMapper(){}
    
    public static NotificationRecipientInt externalToInternal(NotificationRecipient recipient) {

        NotificationRecipientInt.NotificationRecipientIntBuilder notificationRecIntBuilder = NotificationRecipientInt
                .builder()
                .taxId(recipient.getTaxId())
                .internalId(recipient.getInternalId())
                .denomination(recipient.getDenomination());

        it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationDigitalAddress digitalDomicile = recipient.getDigitalDomicile();
        if(digitalDomicile != null){
            LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE typeEnum = LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.valueOf(digitalDomicile.getType().name());

            notificationRecIntBuilder
                    .digitalDomicile(
                            LegalDigitalAddressInt.builder()
                                    .address(digitalDomicile.getAddress())
                                    .type(typeEnum)
                                    .build()
                    );
        }

        NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();
        if(physicalAddress != null){
            notificationRecIntBuilder
                    .physicalAddress(
                            PhysicalAddressInt.builder()
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
                    .noticeCode( payment.getNoticeCode() )
                    .creditorTaxId( payment.getCreditorTaxId() )
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

            if(payment.getF24standard() != null){
                paymentInfoBuilder
                        .f24standard(
                                NotificationDocumentInt.builder()
                                        .digests(
                                                NotificationDocumentInt.Digests.builder()
                                                        .sha256(payment.getF24standard().getDigests().getSha256())
                                                        .build()
                                        )
                                        .ref(
                                                NotificationDocumentInt.Ref.builder()
                                                        .key(payment.getF24standard().getRef().getKey())
                                                        .versionToken(payment.getF24standard().getRef().getVersionToken())
                                                        .build()
                                        )
                                        .build()
                        );
            }



            notificationRecIntBuilder.payment(paymentInfoBuilder.build());
        }
        
        return notificationRecIntBuilder.build();
    }

    public static NotificationRecipient internalToExternal(NotificationRecipientInt recipient) {
        NotificationRecipient notificationRecipient = new NotificationRecipient();
        NotificationDigitalAddress notificationDigitalAddress = null;

        LegalDigitalAddressInt internalDigitalDomicile = recipient.getDigitalDomicile();
        if(internalDigitalDomicile != null){
            notificationDigitalAddress = getNotificationDigitalAddress(internalDigitalDomicile);
        }

        NotificationPhysicalAddress physicalAddress = null;
        PhysicalAddressInt internalPhysicalAddress = recipient.getPhysicalAddress();
        if(internalPhysicalAddress != null){
            physicalAddress = getNotificationPhysicalAddress(internalPhysicalAddress);
        }

        NotificationPaymentInfo payment = null;
        NotificationPaymentInfoInt paymentInternal = recipient.getPayment();
        if(paymentInternal != null){
            payment = getNotificationPaymentInfo(paymentInternal);
        }

        notificationRecipient.setTaxId(recipient.getTaxId());
        notificationRecipient.setDenomination(recipient.getDenomination());
        notificationRecipient.setDigitalDomicile(notificationDigitalAddress);
        notificationRecipient.setPhysicalAddress(physicalAddress);
        notificationRecipient.setPayment(payment);
        notificationRecipient.setInternalId(recipient.getInternalId());
        
        return notificationRecipient;
    }

    @NotNull
    private static NotificationDigitalAddress getNotificationDigitalAddress(LegalDigitalAddressInt internalDigitalDomicile) {
        NotificationDigitalAddress notificationDigitalAddress;
        notificationDigitalAddress = new NotificationDigitalAddress();
        notificationDigitalAddress.setAddress(internalDigitalDomicile.getAddress());
        notificationDigitalAddress.setType(NotificationDigitalAddress.TypeEnum.valueOf(internalDigitalDomicile.getType().getValue()));
        return notificationDigitalAddress;
    }

    @NotNull
    private static NotificationPaymentInfo getNotificationPaymentInfo(NotificationPaymentInfoInt paymentInternal) {
        NotificationPaymentInfo payment;
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
        return payment;
    }

    @NotNull
    private static NotificationPhysicalAddress getNotificationPhysicalAddress(PhysicalAddressInt internalPhysicalAddress) {
        NotificationPhysicalAddress physicalAddress;
        physicalAddress = new NotificationPhysicalAddress();
        physicalAddress.setAddress(internalPhysicalAddress.getAddress());
        physicalAddress.setAddressDetails(internalPhysicalAddress.getAddressDetails());
        physicalAddress.setAt(internalPhysicalAddress.getAt());
        physicalAddress.setMunicipality(internalPhysicalAddress.getMunicipality());
        physicalAddress.setForeignState(internalPhysicalAddress.getForeignState());
        physicalAddress.setProvince(internalPhysicalAddress.getProvince());
        physicalAddress.setZip(internalPhysicalAddress.getZip());
        return physicalAddress;
    }

}
