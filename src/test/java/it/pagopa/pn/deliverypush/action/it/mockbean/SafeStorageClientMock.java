package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.*;
import it.pagopa.pn.deliverypush.middleware.responsehandler.SafeStorageResponseHandler;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;

@Slf4j
public class SafeStorageClientMock implements PnSafeStorageClient {
    private Map<String, FileCreationWithContentRequest> savedFileMap = new HashMap<>();
    
    private final DocumentCreationRequestService creationRequestService;
    private final SafeStorageResponseHandler safeStorageResponseHandler;
    
    public SafeStorageClientMock(DocumentCreationRequestService creationRequestService, 
                                 SafeStorageResponseHandler safeStorageResponseHandler) {
        this.creationRequestService = creationRequestService;
        this.safeStorageResponseHandler = safeStorageResponseHandler;
    }

    public void clear() {
        this.savedFileMap =  new HashMap<>();
    }

    @Override
    public Mono<FileDownloadResponse> getFile(String fileKey, Boolean metadataOnly) {
        FileCreationWithContentRequest fileCreationWithContentRequest = savedFileMap.get(fileKey);
        
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setContentType(fileCreationWithContentRequest.getContentType());
        fileDownloadResponse.setContentLength(new BigDecimal(0));
        fileDownloadResponse.setChecksum(Base64Utils.encodeToString( fileCreationWithContentRequest.getContent() ));
        fileDownloadResponse.setKey(fileKey);

        FileDownloadInfo downloadInfo = new FileDownloadInfo();
        downloadInfo.setUrl("https://www.url.qualcosa.it");
        downloadInfo.setRetryAfter(new BigDecimal(0));
        fileDownloadResponse.setDownload(downloadInfo);
        
        return Mono.just(fileDownloadResponse);
    }

    @Override
    public Mono<FileCreationResponse> createFile(FileCreationWithContentRequest fileCreationRequest, String sha256) {
        log.info("[TEST] createFile documentType={}", fileCreationRequest.getDocumentType());

        String key = sha256;
        savedFileMap.put(key,fileCreationRequest);

        new Thread(() -> {
            Assertions.assertDoesNotThrow(() -> {
                String keyWithPrefix = FileUtils.getKeyWithStoragePrefix(key);

                log.info("[TEST] Start wait for createFile documentType={} keyWithPrefix={}",fileCreationRequest.getDocumentType(), keyWithPrefix);

                if(! TestUtils.PN_NOTIFICATION_ATTACHMENT.equals(fileCreationRequest.getDocumentType())){
                    try {
                        await().atMost(Duration.ofSeconds(1)).untilAsserted(() ->
                                Assertions.assertTrue(creationRequestService.getDocumentCreationRequest(keyWithPrefix).isPresent())
                        );
                    }catch (org.awaitility.core.ConditionTimeoutException ex){
                        log.error("TEST] Exception in createFile DocumentCreationRequest not founded - fileKey={} documentType={}", keyWithPrefix, fileCreationRequest.getDocumentType());
                        throw ex;
                    }
                    
                    log.info("[TEST] END wait for createFile documentType={} keyWithPrefix={}",fileCreationRequest.getDocumentType(), keyWithPrefix);

                    FileDownloadResponse mockedResponse =  new FileDownloadResponse();
                    mockedResponse.setDocumentType(fileCreationRequest.getDocumentType());
                    mockedResponse.setDocumentStatus(fileCreationRequest.getStatus());
                    mockedResponse.setKey(key);
                    mockedResponse.setChecksum(sha256);
                    safeStorageResponseHandler.handleSafeStorageResponse(mockedResponse);
                } else{
                    log.info("[TEST] No need to wait response for PN_NOTIFICATION_ATTACHMENT");
                }

            });
        }).start();

        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setKey(key);
        fileCreationResponse.setSecret("abc");
        fileCreationResponse.setUploadUrl("https://www.unqualcheurl.it");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);
        
        return Mono.just(fileCreationResponse);
    }

    @Override
    public Mono<OperationResultCodeResponse> updateFileMetadata(String fileKey, UpdateFileMetadataRequest request) {
        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        operationResultCodeResponse.setResultCode("200.00");
        operationResultCodeResponse.setResultDescription("OK");
        
        return Mono.just(operationResultCodeResponse);
    }

    @Override
    public void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256) {
        log.info("[TEST] Upload content Mock - key={} uploadUrl={}", fileCreationResponse.getKey(), fileCreationResponse.getUploadUrl());

    }

    public void writeFile(String fileKey, LegalFactCategoryInt legalFactCategory, String testName){
        FileCreationWithContentRequest fileCreationRequest = savedFileMap.get(fileKey);

        String ext = getExtensionFromContentType(fileCreationRequest.getContentType());
        String TEST_DIR_NAME = "target" + File.separator + "generated-test-PDF-IT";
        Path TEST_DIR_PATH = Paths.get(TEST_DIR_NAME);

        //create target test folder, if not exists
        if (Files.notExists(TEST_DIR_PATH)) {
            try {
                Files.createDirectory(TEST_DIR_PATH);
            } catch (IOException e) {
                System.out.println("Exception in uploadContent " + e);
            }
        }

        Path filePath = Paths.get(TEST_DIR_NAME + File.separator + testName+ "-"+ legalFactCategory.getValue() + "." + ext);
        try {
            Files.write(filePath, fileCreationRequest.getContent());
        } catch (IOException e) {
            System.out.println("Exception in uploadContent " + e);
        }
    }
    
    private String getExtensionFromContentType(String contentType) {
        switch (contentType){
            case "application/pdf":
                return "pdf";
            case "text/html":
                return "html";
            default:
                System.out.println("Content type not expected "+contentType);
                return "pdf";
        }
    }
}
