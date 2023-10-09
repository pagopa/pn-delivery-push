package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnF24MetadataValidationEndEvent;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.f24.PnF24Client;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.responsehandler.F24ResponseHandler;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
@Configuration
@AllArgsConstructor
@CustomLog
public class F24EventHandler {

    private F24ResponseHandler handler;

    @Bean
    public Consumer<Message<PnF24MetadataValidationEndEvent.Detail>> pnF24EventInboundConsumer() {
        return message -> {
            try {
                log.debug("Handle message from {} with content {}", PnF24Client.CLIENT_NAME, message);
                PnF24MetadataValidationEndEvent.Detail event = message.getPayload();
                handler.handleResponseReceived(event);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }

        };
    }
}
