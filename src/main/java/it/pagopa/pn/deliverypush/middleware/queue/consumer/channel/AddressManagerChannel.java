package it.pagopa.pn.deliverypush.middleware.queue.consumer.channel;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.addressmanager.model.NormalizeItemsResult;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.addressmanager.AddressManagerClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
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
public class AddressManagerChannel {
    private AddressManagerResponseHandler handler;
    
    @Bean
    public Consumer<Message<NormalizeItemsResult>> pnAddressManagerEventInboundConsumer() {
        return ChannelWrapper.withMDC(message -> {
            try {
                log.debug("Handle message from {} with content {}", AddressManagerClient.CLIENT_NAME, message);
                NormalizeItemsResult response = message.getPayload();

                handler.handleResponseReceived(response);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
            
        });
    }
   
}
