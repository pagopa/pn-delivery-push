package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.*;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RecipientMapperTest {

    @Test
    void externalToInternal() {
        //GIVEN
        NotificationRecipient given = buildNotificationRecipient();
        
        //WHEN
        NotificationRecipientInt actual = RecipientMapper.externalToInternal(given);
        
        //THEN
        NotificationRecipientInt expected = buildNotificationRecipientInt();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void internalToExternal() {

        NotificationRecipientInt given = buildNotificationRecipientInt();

        NotificationRecipient actual = RecipientMapper.internalToExternal(given);

        NotificationRecipient expected = buildNotificationRecipient();

        Assertions.assertEquals(expected, actual);
    }

    private NotificationRecipientInt buildNotificationRecipientInt() {
        return NotificationRecipientInt.builder()
                .taxId("001")
                .internalId("002")
                .denomination("003")
                .digitalDomicile(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("account@dominio.it")
                                .build()
                )
                .physicalAddress(PhysicalAddressInt.builder()
                        .at("003")
                        .address("001")
                        .addressDetails("002")
                        .zip("007")
                        .municipality("004")
                        .province("006")
                        .foreignState("005")
                        .build())
                .payment(NotificationPaymentInfoInt.builder()
                        .noticeCode("001")
                        .creditorTaxId("002")
                        .pagoPaForm(NotificationDocumentInt.builder()
                                .digests(NotificationDocumentInt.Digests.builder().sha256("001").build())
                                .ref(NotificationDocumentInt.Ref.builder()
                                        .key("001")
                                        .versionToken("002")
                                        .build())
                                .build())
                        .f24flatRate(NotificationDocumentInt.builder()
                                .digests(NotificationDocumentInt.Digests.builder().sha256("001").build())
                                .ref(NotificationDocumentInt.Ref.builder()
                                        .key("001")
                                        .versionToken("002")
                                        .build())
                                .build())
                        .f24standard(NotificationDocumentInt.builder()
                                .digests(NotificationDocumentInt.Digests.builder().sha256("001").build())
                                .ref(NotificationDocumentInt.Ref.builder()
                                        .key("001")
                                        .versionToken("002")
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private NotificationRecipient buildNotificationRecipient() {
        NotificationPhysicalAddress physicalAddress = new NotificationPhysicalAddress();
        physicalAddress.setAddress("001");
        physicalAddress.setAddressDetails("002");
        physicalAddress.setAt("003");
        physicalAddress.setMunicipality("004");
        physicalAddress.setForeignState("005");
        physicalAddress.setProvince("006");
        physicalAddress.setZip("007");

        NotificationPaymentAttachment pagoPaForm = new NotificationPaymentAttachment();
        NotificationAttachmentDigests digests = new NotificationAttachmentDigests();
        digests.setSha256("001");
        pagoPaForm.setDigests(digests);
        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey("001");
        ref.setVersionToken("002");
        pagoPaForm.setRef(ref);

        NotificationPaymentAttachment f24FlatRate = new NotificationPaymentAttachment();
        NotificationAttachmentDigests digestsF24FlatRate = new NotificationAttachmentDigests();
        digestsF24FlatRate.setSha256("001");
        f24FlatRate.setDigests(digestsF24FlatRate);
        NotificationAttachmentBodyRef refF24FlatRate = new NotificationAttachmentBodyRef();
        refF24FlatRate.setKey("001");
        refF24FlatRate.setVersionToken("002");
        f24FlatRate.setRef(refF24FlatRate);

        NotificationPaymentAttachment f24Standard = new NotificationPaymentAttachment();
        NotificationAttachmentDigests digestsF24Standard = new NotificationAttachmentDigests();
        digestsF24Standard.setSha256("001");
        f24Standard.setDigests(digestsF24Standard);
        NotificationAttachmentBodyRef refF24Standard = new NotificationAttachmentBodyRef();
        refF24Standard.setKey("001");
        refF24Standard.setVersionToken("002");
        f24Standard.setRef(refF24Standard);
        
        NotificationPaymentInfo paymentInfo = new NotificationPaymentInfo();
        paymentInfo.setNoticeCode("001");
        paymentInfo.setCreditorTaxId("002");
        paymentInfo.setPagoPaForm(pagoPaForm);
        paymentInfo.setF24flatRate(f24FlatRate);
        paymentInfo.setF24standard(f24Standard);
        
        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setTaxId("001");
        recipient.setInternalId("002");
        recipient.setDenomination("003");
        recipient.setDigitalDomicile(new NotificationDigitalAddress()
                .address("account@dominio.it")
                .type(NotificationDigitalAddress.TypeEnum.PEC));
        recipient.setPhysicalAddress(physicalAddress);
        recipient.setPayment(paymentInfo);
        return recipient;
    }
    
}
