package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.*;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static it.pagopa.pn.deliverypush.service.mapper.PaymentMapper.getNotificationPaymentInfo;
import static it.pagopa.pn.deliverypush.service.mapper.PaymentMapper.getNotificationPaymentItem;

public class RecipientMapper {
    private RecipientMapper() {
    }

    public static NotificationRecipientInt externalToInternal(NotificationRecipientV21 recipient) {

        NotificationRecipientInt.NotificationRecipientIntBuilder notificationRecIntBuilder = NotificationRecipientInt
                .builder()
                .taxId(recipient.getTaxId())
                .internalId(recipient.getInternalId())
                .denomination(recipient.getDenomination())
                .recipientType(RecipientTypeInt.valueOf(recipient.getRecipientType().name()));
        it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationDigitalAddress digitalDomicile = recipient.getDigitalDomicile();
        if (digitalDomicile != null) {
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
        notificationRecIntBuilder
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .fullname(recipient.getDenomination())
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


        List<NotificationPaymentInfoIntV2> payments = null;
        List<NotificationPaymentItem> paymentItemList = recipient.getPayments();
        if (!CollectionUtils.isEmpty(paymentItemList)) {
            payments = getNotificationPaymentInfo(paymentItemList);
        }
        notificationRecIntBuilder.payments(payments);

        return notificationRecIntBuilder.build();
    }

    public static NotificationRecipientV21 internalToExternal(NotificationRecipientInt recipient) {
        NotificationRecipientV21 notificationRecipient = new NotificationRecipientV21();
        NotificationDigitalAddress notificationDigitalAddress = null;

        LegalDigitalAddressInt internalDigitalDomicile = recipient.getDigitalDomicile();
        if (internalDigitalDomicile != null) {
            notificationDigitalAddress = getNotificationDigitalAddress(internalDigitalDomicile);
        }

        NotificationPhysicalAddress physicalAddress = null;
        PhysicalAddressInt internalPhysicalAddress = recipient.getPhysicalAddress();
        if (internalPhysicalAddress != null) {
            physicalAddress = getNotificationPhysicalAddress(internalPhysicalAddress);
        }

        List<NotificationPaymentItem> payment = null;
        List<NotificationPaymentInfoIntV2> paymentInternalList = recipient.getPayments();
        if (!CollectionUtils.isEmpty(paymentInternalList)) {
            payment = getNotificationPaymentItem(paymentInternalList);
        }

        notificationRecipient.setTaxId(recipient.getTaxId());
        notificationRecipient.setDenomination(recipient.getDenomination());
        notificationRecipient.setDigitalDomicile(notificationDigitalAddress);
        notificationRecipient.setPhysicalAddress(physicalAddress);
        notificationRecipient.setPayments(payment);
        notificationRecipient.setInternalId(recipient.getInternalId());
        notificationRecipient.setRecipientType(NotificationRecipientV21.RecipientTypeEnum.valueOf(recipient.getRecipientType().name()));

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
    private static NotificationPhysicalAddress getNotificationPhysicalAddress(PhysicalAddressInt internalPhysicalAddress) {
        NotificationPhysicalAddress physicalAddress;
        physicalAddress = new NotificationPhysicalAddress();
        physicalAddress.setAddress(internalPhysicalAddress.getAddress());
        physicalAddress.setAddressDetails(internalPhysicalAddress.getAddressDetails());
        physicalAddress.setAt(internalPhysicalAddress.getAt());
        physicalAddress.setMunicipality(internalPhysicalAddress.getMunicipality());
        physicalAddress.setMunicipalityDetails(internalPhysicalAddress.getMunicipalityDetails());
        physicalAddress.setForeignState(internalPhysicalAddress.getForeignState());
        physicalAddress.setProvince(internalPhysicalAddress.getProvince());
        physicalAddress.setZip(internalPhysicalAddress.getZip());
        return physicalAddress;
    }

}
