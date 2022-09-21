package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.*;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

class RecipientMapperTest {

    @Test
    void externalToInternal() {

        NotificationRecipient given = buildNotificationRecipient();

        NotificationRecipientInt actual = RecipientMapper.externalToInternal(given);

        NotificationRecipientInt expected = buildNotificationRecipientInt();

        Assertions.assertEquals(expected.getTaxId(), actual.getTaxId());
    }

    @Test
    void internalToExternal() {

        NotificationRecipientInt given = buildNotificationRecipientInt();

        NotificationRecipient actual = RecipientMapper.internalToExternal(given);

        NotificationRecipient expected = buildNotificationRecipient();

        Assertions.assertEquals(expected.getTaxId(), actual.getTaxId());
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

        NotificationPaymentInfo paymentInfo = new NotificationPaymentInfo();
        paymentInfo.setNoticeCode("001");
        paymentInfo.setCreditorTaxId("002");
        paymentInfo.setPagoPaForm(pagoPaForm);

        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setTaxId("001");
        recipient.setInternalId("002");
        recipient.setDenomination("003");
        recipient.setDigitalDomicile(new NotificationDigitalAddress()
                .address("address")
                .type(NotificationDigitalAddress.TypeEnum.PEC));
        recipient.setPhysicalAddress(physicalAddress);
        recipient.setPayment(paymentInfo);
        return recipient;
    }

    private SentNotification getExternalNotification() {
        return new SentNotification()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .senderPaId("pa_02")
                .physicalCommunicationType(SentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .amount(18)
                .paymentExpirationDate("2022-10-22")
                .recipients(Collections.singletonList(
                        new NotificationRecipient()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(
                                        new NotificationDigitalAddress()
                                                .address("address")
                                                .type(NotificationDigitalAddress.TypeEnum.PEC)
                                )
                ))
                .documents(Arrays.asList(
                        new NotificationDocument()
                                .ref(new NotificationAttachmentBodyRef()
                                        .key("doc00")
                                        .versionToken("v01_doc00")
                                )
                                .digests(new NotificationAttachmentDigests()
                                        .sha256("sha256_doc00")
                                ),
                        new NotificationDocument()
                                .ref(new NotificationAttachmentBodyRef()
                                        .key("doc01")
                                        .versionToken("v01_doc01")
                                )
                                .digests(new NotificationAttachmentDigests()
                                        .sha256("sha256_doc01")
                                )
                ));
    }
}