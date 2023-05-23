package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.PaperChannelUpdate;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PaperChannelResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class PaperChannelEventHandler {
   private final PaperChannelResponseHandler paperChannelResponseHandler;

    public PaperChannelEventHandler(PaperChannelResponseHandler paperChannelResponseHandler) {
        this.paperChannelResponseHandler = paperChannelResponseHandler;
    }
    
    @Bean
    public Consumer<Message<PaperChannelUpdate>> pnPaperChannelEventInboundConsumer() {
        return message -> {
            try {
                log.debug("Handle message from {} with content {}", PaperChannelSendClient.CLIENT_NAME, message);
                
                PaperChannelUpdate singleStatusUpdate = message.getPayload();
                paperChannelResponseHandler.paperChannelResponseReceiver(singleStatusUpdate);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
