package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.addressmanager.generated.openapi.clients.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager.AddressManagerClient;
import it.pagopa.pn.deliverypush.middleware.responsehandler.AddressManagerResponseHandler;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@CustomLog
public class AddressManagerEventHandler {
    private AddressManagerResponseHandler handler;
    
    @Bean
    public Consumer<Message<NormalizeItemsResult>> pnAddressManagerEventInboundConsumer() {
        return message -> {
            log.debug("Handle message from {} with content {}", AddressManagerClient.CLIENT_NAME, message);
            NormalizeItemsResult response = message.getPayload();

            handler.handleResponseReceived(response);
        };
    }
   
}
