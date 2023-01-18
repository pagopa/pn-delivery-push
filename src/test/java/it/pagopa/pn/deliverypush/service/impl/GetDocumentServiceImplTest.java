package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GetDocumentServiceImplTest {

    @InjectMocks
    private GetDocumentServiceImpl getDocumentService;
    
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuthUtils authUtils;
    @Mock
    private SafeStorageService safeStorageService;

    @Test
    void getDocumentMetadata() {
        //GIVEN
        String iun = "testIun";
        DocumentCategory documentType = DocumentCategory.AAR;
        String documentId = "testDocumentId";
        String recipientId = "testRecipientId";
        String taxId = "TAX_ID";

        NotificationRecipientInt recipientInt = buildNotificationRecipientInt(taxId, recipientId);

        NotificationInt notification = buildNotificationInt(iun, recipientInt);

        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.anyString()))
                        .thenReturn(Mono.just(notification));

        FileDownloadResponseInt fileDownloadResponse = buildFileDownloadResponseInt();

        
        Mockito.when(safeStorageService.getFileReactive(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(Mono.just(fileDownloadResponse));
        
        //WHEN
        Mono<DocumentDownloadMetadataResponse> responseMono = getDocumentService.getDocumentMetadata(iun, documentType, documentId, recipientId);
        
        //THEN
        DocumentDownloadMetadataResponse downloadMetadataResponse = responseMono.block();
        
        assertNotNull( downloadMetadataResponse );
        assertNotNull(downloadMetadataResponse.getFilename());
        assertEquals(fileDownloadResponse.getDownload().getUrl(), downloadMetadataResponse.getUrl());
        assertEquals(fileDownloadResponse.getDownload().getRetryAfter(), downloadMetadataResponse.getRetryAfter());
        assertEquals(fileDownloadResponse.getContentLength(), downloadMetadataResponse.getContentLength());
    }

    @Test
    void getDocumentWebMetadataSuccessTest() {
        //GIVEN
        String iun = "testIun";
        DocumentCategory documentType = DocumentCategory.AAR;
        String documentId = "testDocumentId";
        String recipientId = "ANON";
        String taxId = "TAX_ID";

        NotificationRecipientInt recipientInt = buildNotificationRecipientInt(taxId, recipientId);

        NotificationInt notification = buildNotificationInt(iun, recipientInt);

        FileDownloadResponseInt fileDownloadResponse = buildFileDownloadResponseInt();


        //WHEN
        Mockito.when(notificationService.getNotificationByIunReactive(iun))
                .thenReturn(Mono.just(notification));


        Mockito.when(safeStorageService.getFileReactive(documentId, false))
                .thenReturn(Mono.just(fileDownloadResponse));


        //THEN
        Mono<DocumentDownloadMetadataResponse> responseMono = getDocumentService.getDocumentWebMetadata(iun, documentType, documentId, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);
        DocumentDownloadMetadataResponse downloadMetadataResponse = responseMono.block();

        assertNotNull( downloadMetadataResponse );
        assertNotNull(downloadMetadataResponse.getFilename());
        assertEquals(fileDownloadResponse.getDownload().getUrl(), downloadMetadataResponse.getUrl());
        assertEquals(fileDownloadResponse.getDownload().getRetryAfter(), downloadMetadataResponse.getRetryAfter());
        assertEquals(fileDownloadResponse.getContentLength(), downloadMetadataResponse.getContentLength());
    }

    @Test
    void getDocumentWebMetadataFailedTest() {
        //GIVEN
        String iun = "testIun";
        DocumentCategory documentType = DocumentCategory.AAR;
        String documentId = "testDocumentId";
        String recipientId = "ANON";
        String taxId = "TAX_ID";
        String message = String.format("User haven't authorization to get required legal facts - iun=%s user=%s", iun, recipientId);

        NotificationRecipientInt recipientInt = buildNotificationRecipientInt(taxId, recipientId);

        NotificationInt notification = buildNotificationInt(iun, recipientInt);
        
        //WHEN
        Mockito.when(notificationService.getNotificationByIunReactive(iun))
                .thenReturn(Mono.just(notification));

        Mockito.doThrow(new PnNotFoundException("Not found", message, ERROR_CODE_DELIVERYPUSH_NOTFOUND))
                .when(authUtils)
                .checkUserPaAndMandateAuthorization(notification, recipientInt.getInternalId(), null, CxTypeAuthFleet.PF, null);

        //THEN
        StepVerifier.create(getDocumentService.getDocumentWebMetadata(iun, documentType, documentId, recipientId, null, CxTypeAuthFleet.PF, null))
                .expectError(PnNotFoundException.class)
                .verify();
    }

    private NotificationRecipientInt buildNotificationRecipientInt(String taxId, String recipientId) {
        return NotificationRecipientInt.builder()
                .taxId(taxId)
                .internalId(recipientId)
                .build();
    }

    private NotificationInt buildNotificationInt(String iun, NotificationRecipientInt recipientInt) {
        return NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationRecipient(recipientInt)
                .build();
    }

    private FileDownloadResponseInt buildFileDownloadResponseInt() {
        FileDownloadResponseInt fileDownloadResponse = new FileDownloadResponseInt();
        fileDownloadResponse.setContentType("application/xml");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfoInt());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));
        return fileDownloadResponse;
    }
}