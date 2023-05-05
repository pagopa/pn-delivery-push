package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.middleware.responsehandler.AddressManagerResponseHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@Slf4j
public class AddressManagerEventHandler {
    private AddressManagerResponseHandler handler;
    
    @Bean
    public Consumer<Message<NormalizeItemsResult>> pnAddressManagerEventInboundConsumer() {
        return message -> {
            log.info("Address manager event received, message {}", message);
            NormalizeItemsResult response = message.getPayload();

            handler.handleResponseReceived(response);
        };
    }
   
}
