package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.NotificationChannelType;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendAttachmentMode;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotMatchingShaException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.legalfacts.DocumentComposition;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import it.pagopa.pn.deliverypush.utils.PnSendMode;
import it.pagopa.pn.deliverypush.utils.PnSendModeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Base64Utils;
import org.springframework.util.unit.DataSize;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachmentUtilsTest {

    private AttachmentUtils attachmentUtils;

    private SafeStorageService safeStorageService;

    private NotificationUtils notificationUtils;

    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private PnSendModeUtils pnSendModeUtils;

    private AarUtils aarUtils;

    private NotificationProcessCostService notificationProcessCostService;

    @BeforeEach
    void init(){
        safeStorageService = Mockito.mock(SafeStorageService.class);
        pnDeliveryPushConfigs = Mockito.mock(PnDeliveryPushConfigs.class);
        pnSendModeUtils = Mockito.mock(PnSendModeUtils.class);
        notificationProcessCostService = Mockito.mock(NotificationProcessCostService.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        aarUtils = Mockito.mock(AarUtils.class);
        attachmentUtils = new AttachmentUtils(safeStorageService, pnDeliveryPushConfigs, notificationProcessCostService, pnSendModeUtils, aarUtils, notificationUtils);

    }


    @Test
    void validateAttachmentWithoutF24() {
        NotificationInt notification = TestUtils.getNotificationV2();
        when(safeStorageService.getFile(any(), any())).thenReturn(Mono.just(FileDownloadResponseInt.builder()
                .key("key")
                .checksum("sha256")
                .contentLength(BigDecimal.TEN)
                .download(FileDownloadInfoInt.builder().build())
                .contentType("contentType")
                .build()));
        when(safeStorageService.downloadPieceOfContent(any(), any(), anyLong())).thenReturn(Mono.just("%PDF-".getBytes(StandardCharsets.UTF_8)));
        when(pnDeliveryPushConfigs.isCheckPdfValidEnabled()).thenReturn(true);
        when(pnDeliveryPushConfigs.getCheckPdfSize()).thenReturn(DataSize.ofBytes(10));
        Assertions.assertDoesNotThrow(() -> attachmentUtils.validateAttachment(notification));
    }

    @Test
    void validateAttachmentDigestNotMatch() {
        NotificationInt notification = TestUtils.getNotificationV2();
        when(safeStorageService.getFile(any(), any())).thenReturn(Mono.just(FileDownloadResponseInt.builder()
                .key("key")
                .checksum("digest")
                .contentLength(BigDecimal.TEN)
                .download(FileDownloadInfoInt.builder().build())
                .contentType("contentType")
                .build()));
        when(pnDeliveryPushConfigs.isCheckPdfValidEnabled()).thenReturn(true);
        Assertions.assertThrows(PnValidationNotMatchingShaException.class,
                () -> attachmentUtils.validateAttachment(notification));
    }

    @Test
    void validateAttachmentWithF24() {
        NotificationInt notification = TestUtils.getNotificationV2WithF24();
        when(safeStorageService.getFile(any(), any())).thenReturn(Mono.just(FileDownloadResponseInt.builder()
                .key("key")
                .checksum("sha256")
                .contentLength(BigDecimal.TEN)
                .download(FileDownloadInfoInt.builder().build())
                .contentType("contentType")
                .build()));
        when(safeStorageService.downloadPieceOfContent(any(), any(), anyLong())).thenReturn(Mono.just("%PDF-".getBytes(StandardCharsets.UTF_8)));
        when(pnDeliveryPushConfigs.isCheckPdfValidEnabled()).thenReturn(true);
        when(pnDeliveryPushConfigs.getCheckPdfSize()).thenReturn(DataSize.ofBytes(10));

        Assertions.assertDoesNotThrow(() -> attachmentUtils.validateAttachment(notification));
    }

    @Test
    void validateAttachment() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        FileDownloadResponseInt resp1 = new FileDownloadResponseInt();
        resp1.setKey("abcd");
        resp1.setChecksum( "c2hhMjU2X2RvYzAw" );
        resp1.setDownload(FileDownloadInfoInt.builder().build());

        FileDownloadResponseInt resp2 = new FileDownloadResponseInt();
        resp2.setKey("abcd");
        resp2.setChecksum( "c2hhMjU2X2RvYzAx" );
        resp2.setDownload(FileDownloadInfoInt.builder().build());

        FileDownloadResponseInt resp3 = new FileDownloadResponseInt();
        resp3.setKey("keyPagoPaForm");
        resp3.setChecksum( "a2V5UGFnb1BhRm9ybQ==" );
        resp3.setDownload(FileDownloadInfoInt.builder().build());

        //Mockito.doNothing().when(validator).checkPreloadedDigests(Mockito.anyString(), Mockito.any( NotificationDocumentInt.Digests.class), Mockito.any( NotificationDocumentInt.Digests.class));
        Mockito.when(safeStorageService.getFile( "c2hhMjU2X2RvYzAw", false)).thenReturn(Mono.just(resp1));
        Mockito.when(safeStorageService.getFile( "c2hhMjU2X2RvYzAx", false)).thenReturn(Mono.just(resp2));
        Mockito.when(safeStorageService.getFile( "keyPagoPaForm", false)).thenReturn(Mono.just(resp3));
        Mockito.when(pnDeliveryPushConfigs.getCheckPdfSize()).thenReturn(DataSize.ofBytes(-1));


        //WHEN
        attachmentUtils.validateAttachment(notification);

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(2)).getFile(any(), Mockito.anyBoolean());
    }

    @Test
    void validateAttachmentFailDifferentKey() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        FileDownloadResponseInt resp = new FileDownloadResponseInt();
        resp.setKey("abcd");

        Mockito.when(safeStorageService.getFile(any(), Mockito.anyBoolean())).thenReturn(Mono.just(resp));

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

        Mockito.when(safeStorageService.getFile(any(), Mockito.anyBoolean())).thenReturn(Mono.error(new PnNotFoundException("Not found", message, ERROR_CODE_DELIVERYPUSH_NOTFOUND)));

        //THEN
        assertThrows(PnNotFoundException.class, () -> attachmentUtils.validateAttachment(notification));
    }


    @Test
    void validateAttachmentFailTooBig() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        FileDownloadResponseInt resp = new FileDownloadResponseInt();
        resp.setKey("abcd");
        resp.setChecksum( "c2hhMjU2X2RvYzAw" );
        resp.setContentLength(BigDecimal.valueOf(100000));

        Mockito.when(pnDeliveryPushConfigs.getCheckPdfSize()).thenReturn(DataSize.ofBytes(100));
        Mockito.when(safeStorageService.getFile(any(), Mockito.anyBoolean())).thenReturn(Mono.just(resp));

        //THEN
        assertThrows(PnValidationException.class, () -> attachmentUtils.validateAttachment(notification));
    }


    @Test
    void validateAttachmentFailBadFile() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        FileDownloadResponseInt resp = new FileDownloadResponseInt();
        resp.setKey("abcd");
        resp.setChecksum( "c2hhMjU2X2RvYzAw" );
        resp.setContentLength(BigDecimal.valueOf(99));
        resp.setDownload(FileDownloadInfoInt.builder()
                .url("https://fileurl")
                .build());


        Mockito.when(pnDeliveryPushConfigs.getCheckPdfSize()).thenReturn(DataSize.ofBytes(100));
        Mockito.when(pnDeliveryPushConfigs.isCheckPdfValidEnabled()).thenReturn(true);
        Mockito.when(safeStorageService.getFile(any(), Mockito.anyBoolean())).thenReturn(Mono.just(resp));
        Mockito.when(safeStorageService.downloadPieceOfContent(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())).thenReturn(downloadPieceOfContent(false));

        //THEN
        assertThrows(PnValidationException.class, () -> attachmentUtils.validateAttachment(notification));
    }

    @Test
    void validateAttachmentOkFile() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        FileDownloadResponseInt resp = new FileDownloadResponseInt();
        resp.setKey("abcd");
        resp.setChecksum( "c2hhMjU2X2RvYzAw" );
        resp.setContentLength(BigDecimal.valueOf(99));
        resp.setDownload(FileDownloadInfoInt.builder()
                .url("https://fileurl")
                .build());

        FileDownloadResponseInt resp3 = new FileDownloadResponseInt();
        resp3.setKey("keyPagoPaForm");
        resp3.setChecksum( "a2V5UGFnb1BhRm9ybQ==" );
        resp3.setContentLength(BigDecimal.valueOf(99));
        resp3.setDownload(FileDownloadInfoInt.builder()
                .url("https://fileurl")
                .build());

        Mockito.when(pnDeliveryPushConfigs.getCheckPdfSize()).thenReturn(DataSize.ofBytes(100));
        Mockito.when(pnDeliveryPushConfigs.isCheckPdfValidEnabled()).thenReturn(true);
        Mockito.when(safeStorageService.getFile( "c2hhMjU2X2RvYzAw", false)).thenReturn(Mono.just(resp));
        Mockito.when(safeStorageService.getFile( "keyPagoPaForm", false)).thenReturn(Mono.just(resp3));
        Mockito.when(safeStorageService.downloadPieceOfContent(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())).thenReturn(downloadPieceOfContent(true));

        //THEN
        attachmentUtils.validateAttachment(notification);


        //THEN
        Mockito.verify(safeStorageService, Mockito.times(2)).getFile(any(), Mockito.anyBoolean());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void retrieveAttachmentsAAR() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        //WHEN
        List<String> attachments = attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR, false, List.of());

        // THEN
        Assertions.assertNotNull(attachments);
        Assertions.assertFalse(attachments.isEmpty());
        Assertions.assertEquals(1, attachments.size());
        Assertions.assertEquals(aarGenerationDetails.getGeneratedAarUrl(),attachments.get(0));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void retrieveAttachmentsAAR_DOCUMENTS() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2WithDocument();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        //WHEN
        List<String> attachments = attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR_DOCUMENTS, false, List.of());

        // THEN
        Assertions.assertNotNull(attachments);
        Assertions.assertFalse(attachments.isEmpty());
        Assertions.assertEquals(2, attachments.size());
        Assertions.assertEquals(aarGenerationDetails.getGeneratedAarUrl(),attachments.get(0));
        Assertions.assertEquals("safestorage://"+notification.getDocuments().get(0).getRef().getKey(),attachments.get(1));
    }

    @Test
    void retrieveSendAttachmentModeANALOG_NOTIFICATION() {
        //GIVEN
        PnSendMode pnSendMode = PnSendMode.builder()
                .startConfigurationTime(Instant.now())
                .digitalSendAttachmentMode(SendAttachmentMode.AAR)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                .build();
        Mockito.when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);
        //WHEN
        SendAttachmentMode sendAttachmentMode = attachmentUtils.retrieveSendAttachmentMode(TestUtils.getNotificationV2WithDocument(), NotificationChannelType.ANALOG_NOTIFICATION);
        //THEN
       Assertions.assertEquals(sendAttachmentMode,pnSendMode.getAnalogSendAttachmentMode());
    }

    @Test
    void retrieveSendAttachmentModeSIMPLE_REGISTERED_LETTER() {
        //GIVEN
        PnSendMode pnSendMode = PnSendMode.builder()
                .startConfigurationTime(Instant.now())
                .digitalSendAttachmentMode(SendAttachmentMode.AAR)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                .build();
        Mockito.when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);
        //WHEN
        SendAttachmentMode sendAttachmentMode = attachmentUtils.retrieveSendAttachmentMode(TestUtils.getNotificationV2WithDocument(), NotificationChannelType.SIMPLE_REGISTERED_LETTER);
        //THEN
        Assertions.assertEquals(sendAttachmentMode,pnSendMode.getSimpleRegisteredLetterSendAttachmentMode());
    }

    @Test
    void retrieveSendAttachmentModeDIGITAL_NOTIFICATION() {
        //GIVEN
        PnSendMode pnSendMode = PnSendMode.builder()
                .startConfigurationTime(Instant.now())
                .digitalSendAttachmentMode(SendAttachmentMode.AAR)
                .simpleRegisteredLetterSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS)
                .analogSendAttachmentMode(SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS)
                .aarTemplateType(DocumentComposition.TemplateType.AAR_NOTIFICATION)
                .build();
        Mockito.when(pnSendModeUtils.getPnSendMode(any())).thenReturn(pnSendMode);
        //WHEN
        SendAttachmentMode sendAttachmentMode = attachmentUtils.retrieveSendAttachmentMode(TestUtils.getNotificationV2WithDocument(), NotificationChannelType.DIGITAL_NOTIFICATION);
        //THEN
        Assertions.assertEquals(sendAttachmentMode,pnSendMode.getDigitalSendAttachmentMode());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void retrieveAttachmentsAAR_DOCUMENTS_PAYMENTS() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotificationV2WithDocument();

        AarGenerationDetailsInt aarGenerationDetails = AarGenerationDetailsInt.builder()
                .generatedAarUrl("http").build();
        Mockito.when(aarUtils.getAarGenerationDetails(any(), Mockito.anyInt())).thenReturn(aarGenerationDetails);

        Mockito.when(notificationUtils.getRecipientFromIndex(any(),anyInt())).thenReturn(notification.getRecipients().get(0));

        //WHEN
        List<String> attachments = attachmentUtils.retrieveAttachments(notification, 0, SendAttachmentMode.AAR_DOCUMENTS_PAYMENTS, false, List.of());

        // THEN
        System.out.println(attachments);
        Assertions.assertNotNull(attachments);
        Assertions.assertFalse(attachments.isEmpty());
        Assertions.assertEquals(3, attachments.size());
        Assertions.assertEquals(aarGenerationDetails.getGeneratedAarUrl(),attachments.get(0));
        Assertions.assertEquals("safestorage://"+notification.getDocuments().get(0).getRef().getKey(),attachments.get(1));
        Assertions.assertEquals("safestorage://"+notification.getRecipients().get(0).getPayments().get(0).getPagoPA().getAttachment().getRef().getKey(),
                attachments.get(2));
    }

    @Test
    void changeAttachmentsStatusToAttached() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        UpdateFileMetadataResponseInt resp = new UpdateFileMetadataResponseInt();
        resp.setResultCode("200.00");

        Mockito.when(safeStorageService.updateFileMetadata(any(), any())).thenReturn(Mono.just(resp));

        //WHEN
        attachmentUtils.changeAttachmentsStatusToAttached(notification);

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(2)).updateFileMetadata(any(), any());
    }

    @Test
    void changeAttachmentsStatusToAttachedFail() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);


        Mockito.when(safeStorageService.updateFileMetadata(any(), any())).thenThrow(new PnInternalException("test", "test"));

        //WHEN
        assertThrows(PnInternalException.class, () -> attachmentUtils.changeAttachmentsStatusToAttached(notification));

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(1)).updateFileMetadata(any(), any());
    }

    @Test
    void changeAttachmentsRetention() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        UpdateFileMetadataResponseInt resp = new UpdateFileMetadataResponseInt();
        resp.setResultCode("200.00");

        Mockito.when(safeStorageService.updateFileMetadata(any(), any())).thenReturn(Mono.just(resp));

        //WHEN
        attachmentUtils.changeAttachmentsRetention(notification, 20).blockFirst();

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(2)).updateFileMetadata(any(), any());
    }

    @Test
    void changeAttachmentsRetentionKO() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(safeStorageService.updateFileMetadata(any(), any())).thenThrow(new PnInternalException("test", "test"));

        Flux<Void> flux = attachmentUtils.changeAttachmentsRetention(notification, 20);
        //WHEN
        assertThrows(PnInternalException.class, flux::blockFirst);

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(1)).updateFileMetadata(any(), any());
    }

    @Test
    void changeAttachmentsStatusToAttachedFail400() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        UpdateFileMetadataResponseInt resp = new UpdateFileMetadataResponseInt();
        resp.setResultCode("400.00");

        Mockito.when(safeStorageService.updateFileMetadata(any(), any())).thenReturn(Mono.just(resp));

        //WHEN
        assertThrows(PnInternalException.class, () -> attachmentUtils.changeAttachmentsStatusToAttached(notification));

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(1)).updateFileMetadata(any(), any());
    }

    @Test
    void getAttachmentsByRecipient() {
        //GIVEN
        NotificationInt notification = getNotificationWithMultipleRecipients();

        int recIndexRecipient1 = 0;
        int recIndexRecipient2 = 1;

        Mockito.when(notificationUtils.getRecipientFromIndex(notification, recIndexRecipient1)).thenReturn(notification.getRecipients().get(recIndexRecipient1));
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, recIndexRecipient2)).thenReturn(notification.getRecipients().get(recIndexRecipient2));

        //WHEN
        //la get notificationAttachment recupera solo i documenti
        List<String> attachmentsRecipient1 = attachmentUtils.getNotificationAttachments(notification);
        List<String> attachmentsRecipient2 = attachmentUtils.getNotificationAttachments(notification);

        Assertions.assertEquals(1, attachmentsRecipient1.size());
        Assertions.assertEquals(1, attachmentsRecipient2.size());
        Assertions.assertEquals(attachmentsRecipient1.get(0), FileUtils.getKeyWithStoragePrefix(notification.getDocuments().get(0).getRef().getKey()));
        Assertions.assertEquals(attachmentsRecipient2.get(0), FileUtils.getKeyWithStoragePrefix(notification.getDocuments().get(0).getRef().getKey()));
    }

    @Test
    void getAttachmentsAndPaymentsByRecipient() {
        //GIVEN
        NotificationInt notification = getNotificationWithMultipleRecipients();

        int recIndexRecipient1 = 0;
        int recIndexRecipient2 = 1;

        Mockito.when(notificationProcessCostService.notificationProcessCostF24(any(), anyInt(), any(), any(), any(),any())).thenReturn(Mono.just(2));

        Mockito.when(notificationUtils.getRecipientFromIndex(notification, recIndexRecipient1)).thenReturn(notification.getRecipients().get(recIndexRecipient1));
        Mockito.when(notificationUtils.getRecipientFromIndex(notification, recIndexRecipient2)).thenReturn(notification.getRecipients().get(recIndexRecipient2));

        //WHEN
        List<String> attachmentsRecipient1 = attachmentUtils.getNotificationAttachmentsAndPayments(notification, notification.getRecipients().get(0), 0, true, Collections.emptyList());
        List<String> attachmentsRecipient2 = attachmentUtils.getNotificationAttachmentsAndPayments(notification, notification.getRecipients().get(1), 1, false, Collections.emptyList());

        //THEN
        Assertions.assertEquals(3, attachmentsRecipient1.size());
        Assertions.assertEquals(2, attachmentsRecipient2.size());
        Assertions.assertEquals(attachmentsRecipient1.get(0), FileUtils.getKeyWithStoragePrefix(notification.getDocuments().get(0).getRef().getKey()));
        Assertions.assertEquals(attachmentsRecipient2.get(0), FileUtils.getKeyWithStoragePrefix(notification.getDocuments().get(0).getRef().getKey()));

        Assertions.assertEquals(attachmentsRecipient2.get(1), FileUtils.getKeyWithStoragePrefix(notification.getRecipients().get(recIndexRecipient2).getPayments().get(0).getPagoPA().getAttachment().getRef().getKey()));
    }

    @Test
    void f24UrlTest() {
        //GIVEN
        String iun = "testIun";
        Integer recIndex = 0;
        Integer cost = 10;
        Integer vat = 22;

        //WHEN
        String f24Url = attachmentUtils.getF24Url(iun, recIndex, cost, vat);

        //THEN
        Assertions.assertNotNull(f24Url);
        Assertions.assertTrue(f24Url.contains("?cost="+cost));
        Assertions.assertTrue(f24Url.contains("&vat="+vat));
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
                .withPayments(Collections.singletonList(
                        NotificationPaymentInfoInt.builder()
                                .pagoPA(PagoPaInt.builder()
                                        .attachment(NotificationDocumentInt.builder()
                                                .ref(NotificationDocumentInt.Ref.builder()
                                                        .key("keyPagoPaForm")
                                                        .build())
                                                .digests(NotificationDocumentInt.Digests.builder()
                                                        .sha256(Base64Utils.encodeToString("keyPagoPaForm".getBytes()))
                                                        .build())
                                                .build())
                                        .build())
                                .build()
                ))
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
                .withPayments(
                        Collections.singletonList(
                                NotificationPaymentInfoInt.builder()
                                        .pagoPA(PagoPaInt.builder()
                                                .noticeCode("302011681384967173")
                                                .creditorTaxId("77777777777")
                                                .attachment(NotificationDocumentInt.builder()
                                                        .digests(NotificationDocumentInt.Digests.builder()
                                                                .sha256("PagoPaSha256").build())
                                                        .ref(NotificationDocumentInt.Ref.builder()
                                                                .key("key")
                                                                .build())
                                                        .build())
                                                .build())
                                        .f24(F24Int.builder()
                                                .applyCost(true)
                                                .metadataAttachment(NotificationDocumentInt.builder()
                                                        .digests(NotificationDocumentInt.Digests.builder()
                                                                .sha256("F24Sha256").build())
                                                        .build())
                                                .title("f24Title")
                                                .build())
                                        .build()
                        )
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
                .withPayments(
                        Collections.singletonList(
                                NotificationPaymentInfoInt.builder()
                                        .pagoPA(
                                                PagoPaInt.builder()
                                                        .noticeCode("302011681384967181")
                                                        .creditorTaxId("77777777777")
                                                        .attachment(NotificationDocumentInt.builder()
                                                                .ref(NotificationDocumentInt.Ref.builder()
                                                                        .key("PN_NOTIFICATION_ATTACHMENTS-a75e827e953c4917b6d1beaf6df56755.pdf")
                                                                        .versionToken("v1")
                                                                        .build())
                                                                .digests(NotificationDocumentInt.Digests.builder()
                                                                        .sha256("jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=")
                                                                        .build())
                                                                .build())
                                                        .build()
                                        )
                                        .f24(F24Int.builder()
                                                .applyCost(true)
                                                .metadataAttachment(NotificationDocumentInt.builder()
                                                        .digests(NotificationDocumentInt.Digests.builder()
                                                                .sha256("F24Sha256").build())
                                                        .build())
                                                .title("f24Title")
                                                .build())
                                        .build()
                        )
                ).build();

        return NotificationInt.builder()
                .iun("iun")
                .paProtocolNumber("302011681384967158")
                .subject("notifica analogica con cucumber")
                .paFee(1)
                .vat(22)
                .physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER)
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
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
    }

    public Mono<byte[]> downloadPieceOfContent(boolean isPdf) {
        byte[] res = new byte[8];
        res[0] = 0x25;
        res[1] = 0x50;
        res[2] = 0x44;
        res[3] = 0x46;
        res[4] = 0x2D;
        res[5] = 0x2D;
        res[6] = 0x2D;
        res[7] = 0x2D;

        if (!isPdf)
            res[1] = 0x2D;

        return Mono.just(res);
    }
}
