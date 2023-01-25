package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class SafeStorageEventHandler {

    public SafeStorageEventHandler() {
    }

    @Bean
    public Consumer<Message<FileDownloadResponse>> pnSafeStorageEventInboundConsumer() {
        return message -> {
            try{
                log.info("SafeStorage event received, message {}", message);
                
            }catch (Exception ex){
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
