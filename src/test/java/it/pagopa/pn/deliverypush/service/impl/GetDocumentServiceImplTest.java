package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GetDocumentServiceImplTest {
    
    private GetDocumentServiceImpl getDocumentService;
    
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuthUtils authUtils;
    @Mock
    private SafeStorageService safeStorageService;

    @BeforeEach
    void setup() {
        getDocumentService = new GetDocumentServiceImpl(
                notificationService,
                authUtils,
                safeStorageService);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getDocumentMetadata() {
        //GIVEN
        String iun = "testIun";
        DocumentCategory documentType = DocumentCategory.AAR;
        String documentId = "testDocumentId";
        String recipientId = "testRecipientId";

        NotificationRecipientInt recipientInt = NotificationRecipientInt.builder()
                .taxId("TAX_ID")
                .internalId("ANON")
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withNotificationRecipient(recipientInt)
                .build();

        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.anyString()))
                        .thenReturn(Mono.just(notification));


        FileDownloadResponseInt fileDownloadResponse = new FileDownloadResponseInt();
        fileDownloadResponse.setContentType("application/xml");
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum("123");
        fileDownloadResponse.setKey("123");
        fileDownloadResponse.setDownload(new FileDownloadInfoInt());
        fileDownloadResponse.getDownload().setUrl("https://www.url.qualcosa.it");
        fileDownloadResponse.getDownload().setRetryAfter(new BigDecimal(0));
        
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
}