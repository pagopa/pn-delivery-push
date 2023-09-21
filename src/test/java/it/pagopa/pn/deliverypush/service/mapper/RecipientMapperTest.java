package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationRecipientV21.RecipientTypeEnum;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class RecipientMapperTest {

    @Test
    void externalToInternal() {
        //GIVEN
        NotificationRecipientV21 given = buildNotificationRecipient();
        
        //WHEN
        NotificationRecipientInt actual = RecipientMapper.externalToInternal(given);
        
        //THEN
        NotificationRecipientInt expected = buildNotificationRecipientInt();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void internalToExternal() {

        NotificationRecipientInt given = buildNotificationRecipientInt();

        NotificationRecipientV21 actual = RecipientMapper.internalToExternal(given);

        NotificationRecipientV21 expected = buildNotificationRecipient();

        Assertions.assertEquals(expected, actual);
    }

    private NotificationRecipientInt buildNotificationRecipientInt() {
        String denomination = "003";
        return NotificationRecipientInt.builder()
                .recipientType(RecipientTypeInt.PF)
                .taxId("001")
                .internalId("002")
                .denomination(denomination)
                .digitalDomicile(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("account@dominio.it")
                                .build()
                )
                .physicalAddress(PhysicalAddressInt.builder()
                        .fullname(denomination)
                        .at("003")
                        .address("001")
                        .addressDetails("002")
                        .zip("007")
                        .municipality("004")
                        .province("006")
                        .foreignState("005")
                        .build())
                .payments(List.of(NotificationPaymentInfoIntV2.builder()
                        .f24(F24Int.builder()
                                .title("title")
                                .applyCost(true)
                                .metadataAttachment(NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder().build())
                                        .digests(NotificationDocumentInt.Digests.builder().sha256("sha256").build())
                                        .build())
                                .build())
                        .pagoPA(PagoPaInt.builder()
                                .noticeCode("noticeCode")
                                .creditorTaxId("taxID")
                                .applyCost(true)
                                .attachment(NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder().build())
                                        .digests(NotificationDocumentInt.Digests.builder().sha256("sha256").build())
                                        .build()).build())
                        .build()))
                .build();
    }

    private NotificationRecipientV21 buildNotificationRecipient() {
        NotificationPhysicalAddress physicalAddress = new NotificationPhysicalAddress();
        physicalAddress.setAddress("001");
        physicalAddress.setAddressDetails("002");
        physicalAddress.setAt("003");
        physicalAddress.setMunicipality("004");
        physicalAddress.setForeignState("005");
        physicalAddress.setProvince("006");
        physicalAddress.setZip("007");
        
        NotificationRecipientV21 recipient = new NotificationRecipientV21();
        recipient.setRecipientType(RecipientTypeEnum.PF);
        recipient.setTaxId("001");
        recipient.setInternalId("002");
        recipient.setDenomination("003");
        recipient.setDigitalDomicile(new NotificationDigitalAddress()
                .address("account@dominio.it")
                .type(NotificationDigitalAddress.TypeEnum.PEC));
        recipient.setPhysicalAddress(physicalAddress);
        NotificationPaymentItem item = new NotificationPaymentItem();
        F24Payment f24Payment = new F24Payment();
        f24Payment.setTitle("title");
        f24Payment.setApplyCost(true);

        NotificationMetadataAttachment notificationMetadataAttachment = new NotificationMetadataAttachment();
        notificationMetadataAttachment.setRef(new NotificationAttachmentBodyRef());
        NotificationAttachmentDigests digests = new NotificationAttachmentDigests();
        digests.setSha256("sha256");
        notificationMetadataAttachment.setDigests(digests);

        f24Payment.setMetadataAttachment(notificationMetadataAttachment);

        item.setF24(f24Payment);

        PagoPaPayment pagoPaPayment = new PagoPaPayment();
        pagoPaPayment.setNoticeCode("noticeCode");
        pagoPaPayment.setApplyCost(true);
        pagoPaPayment.setCreditorTaxId("taxID");

        NotificationPaymentAttachment notificationPaymentAttachment = new NotificationPaymentAttachment();
        notificationPaymentAttachment.setRef(new NotificationAttachmentBodyRef());
        digests.setSha256("sha256");
        notificationPaymentAttachment.setDigests(digests);

        pagoPaPayment.setAttachment(notificationPaymentAttachment);

        item.setPagoPa(pagoPaPayment);
        recipient.setPayments(List.of(item));
        return recipient;
    }
    
}
