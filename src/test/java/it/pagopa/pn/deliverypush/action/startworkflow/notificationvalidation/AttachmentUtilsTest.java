package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotMatchingShaException;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.unit.DataSize;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttachmentUtilsTest {

    private AttachmentUtils attachmentUtils;

    @Mock
    SafeStorageService safeStorageService;

    @Mock
    PnDeliveryPushConfigs pnDeliveryPushConfigs;

    @BeforeAll
    void init(){
        attachmentUtils = new AttachmentUtils(safeStorageService, pnDeliveryPushConfigs);
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
    void changeAttachmentsStatusToAttached() {
        NotificationInt notification = TestUtils.getNotificationV2WithF24();
        when(safeStorageService.updateFileMetadata(any(), any())).thenReturn(Mono.just(UpdateFileMetadataResponseInt.builder()
                .resultCode("2")
                .resultDescription("desc")
                .build()));
        Assertions.assertDoesNotThrow(() -> attachmentUtils.changeAttachmentsStatusToAttached(notification));
    }

    @Test
    void changeAttachmentsRetention() {
        NotificationInt notification = TestUtils.getNotificationV2WithF24();
        Assertions.assertDoesNotThrow(() -> attachmentUtils.changeAttachmentsRetention(notification, 1));
    }
}
