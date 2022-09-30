package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.*;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SafeStorageClientMock implements PnSafeStorageClient {
    private Map<String, FileCreationWithContentRequest> savedFileMap = new HashMap<>();

    public void clear() {
        this.savedFileMap =  new HashMap<>();
    }

    @Override
    public FileDownloadResponse getFile(String fileKey, Boolean metadataOnly) {
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
        
        return fileDownloadResponse;
    }

    @Override
    public FileCreationResponse createFile(FileCreationWithContentRequest fileCreationRequest, String sha256) {
        String key = sha256;
        savedFileMap.put(key,fileCreationRequest);

        FileCreationResponse fileCreationResponse = new FileCreationResponse();
        fileCreationResponse.setKey(key);
        fileCreationResponse.setSecret("abc");
        fileCreationResponse.setUploadUrl("https://www.unqualcheurl.it");
        fileCreationResponse.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);

        return fileCreationResponse;
    }

    @Override
    public OperationResultCodeResponse updateFileMetadata(String fileKey, UpdateFileMetadataRequest request) {
        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        operationResultCodeResponse.setResultCode("200.00");
        operationResultCodeResponse.setResultDescription("OK");
        
        return operationResultCodeResponse;
    }

    @Override
    public void uploadContent(FileCreationWithContentRequest fileCreationRequest, FileCreationResponse fileCreationResponse, String sha256) {

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
