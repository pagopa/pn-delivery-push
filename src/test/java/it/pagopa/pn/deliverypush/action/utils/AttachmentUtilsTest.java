package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.PnValidationExceptionBuilder;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
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
import it.pagopa.pn.deliverypush.validator.NotificationReceiverValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class AttachmentUtilsTest {

    private AttachmentUtils attachmentUtils;

    @Mock
    private NotificationReceiverValidator validator;
    @Mock
    private SafeStorageService safeStorageService;
    @Mock
    private PnAuditLogBuilder auditLogBuilder;

    @ExtendWith(MockitoExtension.class)
    @BeforeEach
    public void setup() {
        attachmentUtils = new AttachmentUtils(validator, safeStorageService, auditLogBuilder);

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

        FileDownloadResponseInt resp = new FileDownloadResponseInt();
        resp.setKey("abcd");

        Mockito.doNothing().when(validator).checkPreloadedDigests(Mockito.anyString(), Mockito.any( NotificationDocumentInt.Digests.class), Mockito.any( NotificationDocumentInt.Digests.class));
        Mockito.when(safeStorageService.getFile(Mockito.any(), Mockito.anyBoolean())).thenReturn(resp);

        //WHEN
        attachmentUtils.validateAttachment(notification);

        //THEN
        Mockito.verify(safeStorageService, Mockito.times(3)).getFile(Mockito.any(), Mockito.anyBoolean());
        Mockito.verify(validator, Mockito.times(3)).checkPreloadedDigests(Mockito.anyString(), Mockito.any(), Mockito.any());
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

        PnValidationException exception = new PnValidationExceptionBuilder(new ExceptionHelper(Optional.empty()))
                .problemErrorList(List.of(ProblemError.builder().code("TEST").build()))
                .build();

        Mockito.doThrow(exception).when(validator).checkPreloadedDigests(Mockito.anyString(), Mockito.any( NotificationDocumentInt.Digests.class), Mockito.any( NotificationDocumentInt.Digests.class));
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
        Mockito.verify(safeStorageService, Mockito.times(3)).updateFileMetadata(Mockito.any(), Mockito.any());
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
                                .build())
                        .build())
                .build();
    }
}