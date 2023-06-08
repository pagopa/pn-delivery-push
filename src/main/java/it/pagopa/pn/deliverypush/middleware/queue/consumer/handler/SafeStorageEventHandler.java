package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.responsehandler.SafeStorageResponseHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
            try {
                log.debug("Handle message from {} with content {}", PnSafeStorageClient.CLIENT_NAME, message);
                FileDownloadResponse response = message.getPayload();
                MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, response.getKey());

                if(SAFE_STORAGE_DOCUMENT_TYPE_AAR.equals(response.getDocumentType()) ||
                        SAFE_STORAGE_DOCUMENT_TYPE_LEGAL_FACT.equals(response.getDocumentType())) {

                    handler.handleSafeStorageResponse(response);
                } else {
                    log.debug("Safe storage event received is not handled - documentType={}", response.getDocumentType());
                }

                MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
            } catch (Exception ex) {
                MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
   
}
