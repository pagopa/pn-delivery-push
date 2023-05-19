package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.SafeStorageResponseHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_DOCUMENT_TYPE_AAR;
import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_DOCUMENT_TYPE_LEGAL_FACT;

@Configuration
@AllArgsConstructor
@Slf4j
public class SafeStorageEventHandler {
    private SafeStorageResponseHandler handler;
    
    @Bean
    public Consumer<Message<FileDownloadResponse>> pnSafeStorageEventInboundConsumer() {
        return message -> {
            log.debug("Handle message from {} with content {}", PnSafeStorageClient.CLIENT_NAME, message);

            FileDownloadResponse response = message.getPayload();
            
            if(SAFE_STORAGE_DOCUMENT_TYPE_AAR.equals(response.getDocumentType()) ||
                    SAFE_STORAGE_DOCUMENT_TYPE_LEGAL_FACT.equals(response.getDocumentType())) {

                handler.handleSafeStorageResponse(response);
            } else {
                log.debug("Safe storage event received is not handled - documentType={}", response.getDocumentType());
            }
            
        };
    }
   
}
