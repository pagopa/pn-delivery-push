package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
@ConditionalOnProperty( name = "pn.delivery-push.featureflags.externalchannel", havingValue = "new")
public class ExtChannelEventHandlerNew {
   private final ExternalChannelResponseHandler externalChannelResponseHandler;

    public ExtChannelEventHandlerNew(ExternalChannelResponseHandler externalChannelResponseHandler) {
        log.debug("HO CARICATO NEW");
        this.externalChannelResponseHandler = externalChannelResponseHandler;
    }
    
    @Bean
    public Consumer<Message<SingleStatusUpdate>> pnExtChannelEventInboundConsumer() {
        return message -> {
            try {
                log.info("External channel event received NEW, message {}", message);

                SingleStatusUpdate singleStatusUpdate = message.getPayload();
                externalChannelResponseHandler.extChannelResponseReceiver(singleStatusUpdate);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
