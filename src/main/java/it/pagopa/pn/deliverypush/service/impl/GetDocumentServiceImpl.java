package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.service.GetDocumentService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.utils.FileNameUtils;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GetDocumentServiceImpl implements GetDocumentService {
    private final NotificationService notificationService;
    private final AuthUtils authUtils;
    private final SafeStorageService safeStorageService;
    
    public GetDocumentServiceImpl(NotificationService notificationService,
                                  AuthUtils authUtils,
                                  SafeStorageService safeStorageService) {
        this.notificationService = notificationService;
        this.authUtils = authUtils;
        this.safeStorageService = safeStorageService;
    }
    
    @Override
    public Mono<DocumentDownloadMetadataResponse> getDocumentMetadata(String iun,
                                                                     DocumentCategory documentType,
                                                                     String documentId,
                                                                     String recipientId
    ) {
        log.info("Start getDocumentMetadata iun={} recId={} documentId={}", iun, recipientId, documentId );
        return notificationService.getNotificationByIunReactive(iun)
                .flatMap( notification -> {
                    authUtils.checkUserAuthorization(notification, recipientId);
                    return safeStorageService.getFileReactive(documentId, false);
                }).map(
                        fileDownloadResponse -> {
                            DocumentDownloadMetadataResponse response =  generateResponse(iun, documentType, documentId, fileDownloadResponse);
                            log.info( "getDocumentMetadata Success iun={} documentId={}", iun, documentId );
                            return response;
                        }
                );
    }

    @NotNull
    private DocumentDownloadMetadataResponse generateResponse(String iun,
                                                              DocumentCategory documentType, 
                                                              String documentId, 
                                                              FileDownloadResponseInt fileDownloadResponse) {
        String fileName = FileNameUtils.buildFileName(iun, documentType.getValue(), documentId, fileDownloadResponse.getContentType());
        
        return DocumentDownloadMetadataResponse.builder()
                .filename(fileName)
                .contentLength(fileDownloadResponse.getContentLength())
                .retryAfter(fileDownloadResponse.getDownload() != null ? fileDownloadResponse.getDownload().getRetryAfter() : null)
                .url(fileDownloadResponse.getDownload().getUrl())
                .build();
    }
}