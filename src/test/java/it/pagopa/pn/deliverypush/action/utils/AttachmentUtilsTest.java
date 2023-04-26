package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
import static org.junit.jupiter.api.Assertions.assertThrows;


class AttachmentUtilsTest {

    private AttachmentUtils attachmentUtils;

    private SafeStorageService safeStorageService;

    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        safeStorageService = Mockito.mock(SafeStorageService.class);
        attachmentUtils = new AttachmentUtils(safeStorageService);
        notificationUtils = Mockito.mock(NotificationUtils.class);
    }

    @Test
    void validateAttachment() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        FileDownloadResponseInt resp1 = new FileDownloadResponseInt();
        resp1.setKey("abcd");
        resp1.setChecksum( "c2hhMjU2X2RvYzAw" );

        FileDownloadResponseInt resp2 = new FileDownloadResponseInt();
        resp2.setKey("abcd");
        resp2.setChecksum( "c2hhMjU2X2RvYzAx" );

        FileDownloadResponseInt resp3 = new FileDownloadResponseInt();
        resp3.setKey("keyf24flatrate");
        resp3.setChecksum( "a2V5ZjI0ZmxhdHJhdGU=" );

        //Mockito.doNothing().when(validator).checkPreloadedDigests(Mockito.anyString(), Mockito.any( NotificationDocumentInt.Digests.class), Mockito.any( NotificationDocumentInt.Digests.class));
        Mockito.when(safeStorageService.getFile( "c2hhMjU2X2RvYzAw", true)).thenReturn(Mono.just(resp1));
        Mockito.when(safeStorageService.getFile( "c2hhMjU2X2RvYzAx", true)).thenReturn(Mono.just(resp2));
        Mockito.when(safeStorageService.getFile( "keyf24flatrate", true)).thenReturn(Mono.just(resp3));

        //WHEN
        attachmentUtils.validateAttachment(notification);

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(2)).getFile(Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    void validateAttachmentFailDifferentKey() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        FileDownloadResponseInt resp = new FileDownloadResponseInt();
        resp.setKey("abcd");

        Mockito.when(safeStorageService.getFile(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.just(resp));

        //THEN
        assertThrows(PnValidationException.class, () -> attachmentUtils.validateAttachment(notification));
    }

    @Test
    void validateAttachmentFailErrorSafeStorage() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        FileDownloadResponseInt resp = new FileDownloadResponseInt();
        resp.setKey("abcd");

        String message = String.format("Get file failed for - fileKey=%s isMetadataOnly=%b", resp.getKey(), false);

        Mockito.when(safeStorageService.getFile(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mono.error(new PnNotFoundException("Not found", message, ERROR_CODE_DELIVERYPUSH_NOTFOUND)));

        //THEN
        assertThrows(PnNotFoundException.class, () -> attachmentUtils.validateAttachment(notification));
    }
    
    @Test
    void changeAttachmentsStatusToAttached() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        UpdateFileMetadataResponseInt resp = new UpdateFileMetadataResponseInt();
        resp.setResultCode("200.00");

        Mockito.when(safeStorageService.updateFileMetadata(Mockito.any(), Mockito.any())).thenReturn(Mono.just(resp));

        //WHEN
        attachmentUtils.changeAttachmentsStatusToAttached(notification);

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(2)).updateFileMetadata(Mockito.any(), Mockito.any());
    }

    @Test
    void changeAttachmentsStatusToAttachedFail() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);


        Mockito.when(safeStorageService.updateFileMetadata(Mockito.any(), Mockito.any())).thenThrow(new PnInternalException("test", "test"));

        //WHEN
        assertThrows(PnInternalException.class, () -> attachmentUtils.changeAttachmentsStatusToAttached(notification));

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(1)).updateFileMetadata(Mockito.any(), Mockito.any());
    }

    @Test
    void changeAttachmentsRetention() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        UpdateFileMetadataResponseInt resp = new UpdateFileMetadataResponseInt();
        resp.setResultCode("200.00");

        Mockito.when(safeStorageService.updateFileMetadata(Mockito.any(), Mockito.any())).thenReturn(Mono.just(resp));

        //WHEN
        attachmentUtils.changeAttachmentsRetention(notification, 20).blockFirst();

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(2)).updateFileMetadata(Mockito.any(), Mockito.any());
    }
    
    @Test
    void changeAttachmentsRetentionKO() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(safeStorageService.updateFileMetadata(Mockito.any(), Mockito.any())).thenThrow(new PnInternalException("test", "test"));

        Flux<Void> flux = attachmentUtils.changeAttachmentsRetention(notification, 20);
        //WHEN
        assertThrows(PnInternalException.class, flux::blockFirst);

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(1)).updateFileMetadata(Mockito.any(), Mockito.any());
    }

    @Test
    void changeAttachmentsStatusToAttachedFail400() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        UpdateFileMetadataResponseInt resp = new UpdateFileMetadataResponseInt();
        resp.setResultCode("400.00");

        Mockito.when(safeStorageService.updateFileMetadata(Mockito.any(), Mockito.any())).thenReturn(Mono.just(resp));

        //WHEN
        assertThrows(PnInternalException.class, () -> attachmentUtils.changeAttachmentsStatusToAttached(notification));

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(1)).updateFileMetadata(Mockito.any(), Mockito.any());
    }

    @Test
    void getAttachmentsByRecipient() {
        //GIVEN
        NotificationInt notification = getNotificationWithMultipleRecipients();

        Integer recIndexRecipient1 = 0;
        Integer recIndexRecipient2 = 1;

        Mockito.when(notificationUtils.getRecipientFromIndex(notification, recIndexRecipient1)).thenReturn(notification.getRecipients().get(recIndexRecipient1));
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, recIndexRecipient2)).thenReturn(notification.getRecipients().get(recIndexRecipient2));

        //WHEN
        List<String> attachmentsRecipient1 = attachmentUtils.getNotificationAttachments(notification, notificationUtils.getRecipientFromIndex(notification, recIndexRecipient1));
        List<String> attachmentsRecipient2 = attachmentUtils.getNotificationAttachments(notification, notificationUtils.getRecipientFromIndex(notification, recIndexRecipient2));

        Assert.assertEquals(attachmentsRecipient1.size(), 1);
        Assert.assertEquals(attachmentsRecipient2.size(), 2);
        Assert.assertEquals(attachmentsRecipient1.get(0), notification.getDocuments().get(0).getRef().getKey());
        Assert.assertEquals(attachmentsRecipient2.get(0), notification.getDocuments().get(0).getRef().getKey());
        Assert.assertEquals(attachmentsRecipient2.get(1), notification.getRecipients().get(recIndexRecipient2).getPayment().getPagoPaForm().getRef().getKey());
    }

    private NotificationInt getNotificationInt(NotificationRecipientInt recipient) {
        return NotificationTestBuilder.builder()
                .withIun("iun_01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
    }

    private NotificationRecipientInt getNotificationRecipientInt() {
        String taxId = "TaxId";
        return NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_" + taxId)
                .withDigitalDomicile(
                        LegalDigitalAddressInt.builder()
                                .address("address")
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .withPayment(NotificationPaymentInfoInt.builder()
                        .f24flatRate(NotificationDocumentInt.builder()
                                .ref(NotificationDocumentInt.Ref.builder()
                                        .key("keyf24flatrate")
                                        .build())
                                .digests( NotificationDocumentInt.Digests.builder()
                                        .sha256( Base64Utils.encodeToString("keyf24flatrate".getBytes()) )
                                        .build() )
                                .build())
                        .build())
                .build();
    }

    private NotificationInt getNotificationWithMultipleRecipients() {

        NotificationRecipientInt notificationRecipient1 = NotificationRecipientTestBuilder.builder()
                .withRecipientType(RecipientTypeInt.PF)
                .withTaxId("CLMCST42R12D969Z")
                .withInternalId("")
                .withPhysicalAddress(
                        PhysicalAddressInt.builder()
                                .at("Presso")
                                .address("Via@ok_AR")
                                .addressDetails("scala b")
                                .zip("40100")
                                .municipality("Milano")
                                .municipalityDetails("Milano")
                                .province("MI")
                                .foreignState("ITALIA")
                                .build()
                )
                .withPayment(NotificationPaymentInfoInt.builder()
                        .noticeCode("302011681384967173")
                        .creditorTaxId("77777777777")
                        .build()
                )
                .build();

        NotificationRecipientInt notificationRecipient2 = NotificationRecipientTestBuilder.builder()
                .withRecipientType(RecipientTypeInt.PG)
                .withTaxId("MSSLGU51P10A087J")
                .withInternalId("")
                .withDenomination("Cucumber_Society")
                .withPhysicalAddress(
                        PhysicalAddressInt.builder()
                                .at("Presso")
                                .address("Via senza nome")
                                .addressDetails("scala b")
                                .zip("40100")
                                .municipality("Milano")
                                .municipalityDetails("Milano")
                                .province("MI")
                                .foreignState("ITALIA")
                                .build()
                )
                .withDigitalDomicile(
                        LegalDigitalAddressInt.builder()
                                .address("testpagopa2@pnpagopa.postecert.local")
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .withPayment(NotificationPaymentInfoInt.builder()
                        .noticeCode("302011681384967181")
                        .creditorTaxId("77777777777")
                        .pagoPaForm(NotificationDocumentInt.builder()
                                .ref(NotificationDocumentInt.Ref.builder()
                                        .key("PN_NOTIFICATION_ATTACHMENTS-a75e827e953c4917b6d1beaf6df56755.pdf")
                                        .versionToken("v1")
                                        .build())
                                .digests( NotificationDocumentInt.Digests.builder()
                                        .sha256( "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=" )
                                        .build() )
                                .build())
                        .build())
                .build();
        NotificationInt notification =  NotificationInt.builder()
                .paProtocolNumber("302011681384967158")
                .subject("notifica analogica con cucumber")
                .physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER)
                .documents(List.of(NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder()
                                .key("PN_NOTIFICATION_ATTACHMENTS-91a87a946c0d4c1ba17cd2a0037665db.pdf")
                                .versionToken("v1")
                                .build())
                        .digests( NotificationDocumentInt.Digests.builder()
                                .sha256( "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE" )
                                .build() )
                        .build()))
                .sender(NotificationSenderInt.builder()
                        .paTaxId("80016350821")
                        .paDenomination("Comune di palermo")
                        .build())
                .recipients(List.of(notificationRecipient1, notificationRecipient2))
                .build();

        return notification;
    }
}