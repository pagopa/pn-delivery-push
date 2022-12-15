package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClientReactive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

class SafeStorageServiceImplTest {
    @Mock
    private PnSafeStorageClient safeStorageClient;
    @Mock
    private PnSafeStorageClientReactive safeStorageClientReactive;
    
    private SafeStorageServiceImpl safeStorageService;
    
    @BeforeEach
    public void init(){
        safeStorageService = new SafeStorageServiceImpl( safeStorageClient, safeStorageClientReactive);
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void getFile() {
        //GIVEN
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setKey("key");
        fileDownloadResponse.setChecksum("checkSum");
        fileDownloadResponse.setContentType("content");
        fileDownloadResponse.setDocumentStatus("status");
        fileDownloadResponse.setDocumentType("type");
        
        Mockito.when(safeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(fileDownloadResponse);
        
        //WHEN
        FileDownloadResponseInt response = safeStorageService.getFile("test", true);
        
        //THEN
        Assertions.assertNotNull(response);
        Assertions.assertEquals(fileDownloadResponse.getKey(), response.getKey());
        Assertions.assertEquals(fileDownloadResponse.getChecksum(), response.getChecksum());
        Assertions.assertEquals(fileDownloadResponse.getContentType(), response.getContentType());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getFileError() {
        //GIVEN
        Mockito.when(safeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
                .thenThrow( new PnInternalException("test", "test") );

        //WHEN
        Assertions.assertThrows( PnNotFoundException.class, () ->{
            safeStorageService.getFile("test", true);
        });
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void getFileReactive() {
        //GIVEN
        FileDownloadResponse fileDownloadResponseExpected = new FileDownloadResponse();
        fileDownloadResponseExpected.setKey("key");
        fileDownloadResponseExpected.setChecksum("checkSum");
        fileDownloadResponseExpected.setContentType("content");
        fileDownloadResponseExpected.setDocumentStatus("status");
        fileDownloadResponseExpected.setDocumentType("type");

        Mockito.when(safeStorageClientReactive.getFile(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(Mono.just(fileDownloadResponseExpected));

        //WHEN
        Mono<FileDownloadResponseInt> response = safeStorageService.getFileReactive("test", true);

        //THEN
        FileDownloadResponseInt fileDownloadResponse = response.block();
        Assertions.assertNotNull(fileDownloadResponse);
        Assertions.assertEquals(fileDownloadResponseExpected.getKey(), fileDownloadResponse.getKey());
        Assertions.assertEquals(fileDownloadResponseExpected.getChecksum(), fileDownloadResponse.getChecksum());
        Assertions.assertEquals(fileDownloadResponseExpected.getContentType(), fileDownloadResponse.getContentType());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void createAndUploadContent() {
        //GIVEN
        FileCreationWithContentRequest fileCreationWithContentRequest = new FileCreationWithContentRequest();
        fileCreationWithContentRequest.setContent("content".getBytes());

        FileCreationResponse expectedResponse = new FileCreationResponse();
        expectedResponse.setKey("key");
        expectedResponse.setSecret("secret");
        
        Mockito.when(safeStorageClient.createFile(Mockito.any(FileCreationWithContentRequest.class), Mockito.anyString()))
                .thenReturn(expectedResponse);

        //WHEN
        FileCreationResponseInt response = safeStorageService.createAndUploadContent(fileCreationWithContentRequest);

        //THEN
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getKey(), expectedResponse.getKey());
    }
}