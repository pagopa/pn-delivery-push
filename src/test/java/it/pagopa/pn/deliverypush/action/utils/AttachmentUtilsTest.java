package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.UpdateFileMetadataResponseInt;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Base64Utils;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertThrows;


class AttachmentUtilsTest {

    private AttachmentUtils attachmentUtils;

    @Mock
    private SafeStorageService safeStorageService;
    @Mock
    private PnAuditLogBuilder auditLogBuilder;

    @ExtendWith(MockitoExtension.class)
    @BeforeEach
    public void setup() {
        auditLogBuilder = Mockito.mock(PnAuditLogBuilder.class);
        safeStorageService = Mockito.mock(SafeStorageService.class);
        attachmentUtils = new AttachmentUtils(safeStorageService, auditLogBuilder);
    }

    @Test
    void validateAttachment() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        PnAuditLogEvent logEvent = Mockito.mock(PnAuditLogEvent.class);

        Mockito.when(auditLogBuilder.build()).thenReturn(logEvent);
        Mockito.when(auditLogBuilder.iun(Mockito.anyString())).thenReturn(auditLogBuilder);
        Mockito.when(auditLogBuilder.before(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogBuilder);
        Mockito.when(logEvent.generateSuccess()).thenReturn(logEvent);
        Mockito.when(logEvent.generateFailure(Mockito.any(), Mockito.any())).thenReturn(logEvent);

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
        Mockito.when(safeStorageService.getFile( "c2hhMjU2X2RvYzAw", true)).thenReturn(resp1);
        Mockito.when(safeStorageService.getFile( "c2hhMjU2X2RvYzAx", true)).thenReturn(resp2);
        Mockito.when(safeStorageService.getFile( "keyf24flatrate", true)).thenReturn(resp3);

        //WHEN
        attachmentUtils.validateAttachment(notification);

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(2)).getFile(Mockito.any(), Mockito.anyBoolean());
        Mockito.verify(logEvent, Mockito.times(1)).generateSuccess();
        Mockito.verify(logEvent, Mockito.times(0)).generateFailure(Mockito.any(), Mockito.any());
    }

    @Test
    void validateAttachmentFail() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        PnAuditLogEvent logEvent = Mockito.mock(PnAuditLogEvent.class);

        Mockito.when(auditLogBuilder.build()).thenReturn(logEvent);
        Mockito.when(auditLogBuilder.iun(Mockito.anyString())).thenReturn(auditLogBuilder);
        Mockito.when(auditLogBuilder.before(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogBuilder);
        Mockito.when(logEvent.generateSuccess()).thenReturn(logEvent);
        Mockito.when(logEvent.generateFailure(Mockito.any(), Mockito.any())).thenReturn(logEvent);

        FileDownloadResponseInt resp = new FileDownloadResponseInt();
        resp.setKey("abcd");

        Mockito.when(safeStorageService.getFile(Mockito.any(), Mockito.anyBoolean())).thenReturn(resp);

        //WHEN
        assertThrows(PnValidationException.class, () -> attachmentUtils.validateAttachment(notification));

        //THEN
        Mockito.verify(logEvent, Mockito.times(0)).generateSuccess();
        Mockito.verify(logEvent, Mockito.times(1)).generateFailure(Mockito.any(), Mockito.any());
    }

    @Test
    void changeAttachmentsStatusToAttached() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        UpdateFileMetadataResponseInt resp = new UpdateFileMetadataResponseInt();
        resp.setResultCode("200.00");

        Mockito.when(safeStorageService.updateFileMetadata(Mockito.any(), Mockito.any())).thenReturn(resp);

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
    void changeAttachmentsStatusToAttachedFail400() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        UpdateFileMetadataResponseInt resp = new UpdateFileMetadataResponseInt();
        resp.setResultCode("400.00");

        Mockito.when(safeStorageService.updateFileMetadata(Mockito.any(), Mockito.any())).thenReturn(resp);

        //WHEN
        assertThrows(PnInternalException.class, () -> attachmentUtils.changeAttachmentsStatusToAttached(notification));

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(1)).updateFileMetadata(Mockito.any(), Mockito.any());
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
}