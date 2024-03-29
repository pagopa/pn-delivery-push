package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class ExtChannelEventHandlerNew {
   private final ExternalChannelResponseHandler externalChannelResponseHandler;

    public ExtChannelEventHandlerNew(ExternalChannelResponseHandler externalChannelResponseHandler) {
        this.externalChannelResponseHandler = externalChannelResponseHandler;
    }
    
    @Bean
    public Consumer<Message<SingleStatusUpdate>> pnExtChannelEventInboundConsumer() {
        return message -> {
            try {
                log.debug("Handle message from {} with content {}", ExternalChannelSendClient.CLIENT_NAME, message);
                
                SingleStatusUpdate singleStatusUpdate = message.getPayload();
                externalChannelResponseHandler.extChannelResponseReceiver(singleStatusUpdate);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
